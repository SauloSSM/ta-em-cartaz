package br.com.elitedevticket.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.elitedevticket.auth.domain.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthenticateUserTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    private final AuthUser user = new AuthUser(
            UUID.randomUUID(), "customer@demo.test", Role.CUSTOMER, encoder.encode("correct-password"));

    @Test
    void authenticatesWithNormalizedEmailAndBcryptPassword() {
        AuthenticateUser useCase = new AuthenticateUser(
                email -> email.equals(user.email()) ? Optional.of(user) : Optional.empty(), encoder);

        AuthUser authenticated = useCase.authenticate("  CUSTOMER@DEMO.TEST ", "correct-password");

        assertThat(authenticated).isEqualTo(user);
        assertThat(authenticated.passwordHash()).startsWith("$2a$10$");
    }

    @Test
    void unknownEmailAndWrongPasswordProduceTheSameSafeError() {
        AuthenticateUser useCase = new AuthenticateUser(email -> Optional.empty(), encoder);

        assertThatThrownBy(() -> useCase.authenticate("missing@demo.test", "wrong"))
                .isExactlyInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais inválidas.");
    }

    @Test
    void wrongPasswordForExistingUserProducesTheSameSafeError() {
        AuthenticateUser useCase = new AuthenticateUser(email -> Optional.of(user), encoder);

        assertThatThrownBy(() -> useCase.authenticate(user.email(), "wrong"))
                .isExactlyInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais inválidas.");
    }
}
