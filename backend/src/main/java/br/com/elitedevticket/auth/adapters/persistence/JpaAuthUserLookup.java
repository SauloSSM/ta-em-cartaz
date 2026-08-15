package br.com.elitedevticket.auth.adapters.persistence;

import br.com.elitedevticket.auth.application.AuthUser;
import br.com.elitedevticket.auth.application.AuthUserLookup;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuthUserLookup implements AuthUserLookup {
    private final UserRepository users;

    public JpaAuthUserLookup(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .map(user -> new AuthUser(user.id(), user.email(), user.role(), user.passwordHash()));
    }
}
