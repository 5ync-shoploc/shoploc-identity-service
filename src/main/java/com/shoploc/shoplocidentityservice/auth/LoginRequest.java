package com.shoploc.shoplocidentityservice.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Identifiants envoyés pour se connecter (US 1.2).
 */
public record LoginRequest(
        @Schema(example = "julie.dupont@mail.fr") @NotBlank String email,
        @Schema(example = "motdepasse") @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
