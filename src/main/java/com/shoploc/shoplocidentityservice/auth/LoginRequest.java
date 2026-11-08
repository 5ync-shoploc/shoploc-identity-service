package com.shoploc.shoplocidentityservice.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Identifiants envoyés pour se connecter (US 1.2).
 */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
