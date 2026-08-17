package br.com.elitedevticket.auth.http;

import br.com.elitedevticket.auth.adapters.security.CsrfTokenRotation;
import br.com.elitedevticket.auth.adapters.security.JwtService;
import br.com.elitedevticket.auth.adapters.security.SessionCookieService;
import br.com.elitedevticket.auth.application.AuthenticateUser;
import br.com.elitedevticket.auth.domain.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public final class AuthController {
    private final AuthenticateUser authenticateUser;
    private final JwtService jwtService;
    private final SessionCookieService cookies;
    private final CsrfTokenRotation csrf;

    public AuthController(
            AuthenticateUser authenticateUser,
            JwtService jwtService,
            SessionCookieService cookies,
            CsrfTokenRotation csrf) {
        this.authenticateUser = authenticateUser;
        this.jwtService = jwtService;
        this.cookies = cookies;
        this.csrf = csrf;
    }

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication, CsrfToken csrfToken) {
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser user)) {
            return new AnonymousSessionResponse();
        }
        return new AuthenticatedSessionResponse(UserResponse.from(user));
    }

    @PostMapping("/login")
    public SessionResponse login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        request.validate();
        var user = authenticateUser.authenticate(request.email(), request.password());
        cookies.set(response, jwtService.issue(user));
        csrf.rotate(httpRequest, response);
        return new AuthenticatedSessionResponse(UserResponse.from(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        cookies.clear(response);
        csrf.rotate(request, response);
        return ResponseEntity.noContent().build();
    }
}
