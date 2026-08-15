package br.com.elitedevticket.auth.application;

import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public final class AuthenticateUser {
    private final AuthUserLookup users;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public AuthenticateUser(AuthUserLookup users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("unused-authentication-value");
    }

    public AuthUser authenticate(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String suppliedPassword = password == null ? "" : password;
        var user = users.findByEmail(normalizedEmail);
        String hash = user.map(AuthUser::passwordHash).orElse(dummyHash);

        if (!passwordEncoder.matches(suppliedPassword, hash) || user.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        return user.orElseThrow();
    }
}
