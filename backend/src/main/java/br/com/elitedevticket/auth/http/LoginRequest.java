package br.com.elitedevticket.auth.http;

import java.util.ArrayList;
import java.util.regex.Pattern;

public record LoginRequest(String email, String password) {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    void validate() {
        var errors = new ArrayList<FieldErrorResponse>();
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isBlank()) {
            errors.add(new FieldErrorResponse("email", "Informe o e-mail."));
        } else if (normalizedEmail.length() > 320 || !EMAIL.matcher(normalizedEmail).matches()) {
            errors.add(new FieldErrorResponse("email", "Informe um e-mail válido."));
        }
        if (password == null) {
            errors.add(new FieldErrorResponse("password", "Informe a senha."));
        }
        if (!errors.isEmpty()) {
            throw new InvalidAuthRequestException(errors);
        }
    }
}
