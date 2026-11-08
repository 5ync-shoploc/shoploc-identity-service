package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Données envoyées par le visiteur pour créer son compte (US 1.1).
 */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull Role role) {

    @Override
    public String toString() {
        // Ne jamais écrire le mot de passe dans les logs
        return "RegisterRequest[email=" + email + ", role=" + role + "]";
    }
}
