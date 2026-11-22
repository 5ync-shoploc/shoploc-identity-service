package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Compte créé, renvoyé au client (sans le mot de passe).
 */
public record RegisterResponse(
        @Schema(description = "Identifiant Keycloak de l'utilisateur (sub du futur JWT)",
                example = "56718d2d-f5ec-45f3-beeb-c08d8d7951b8")
        String id,

        @Schema(example = "Dupont") String lastName,
        @Schema(example = "Julie") String firstName,
        @Schema(example = "julie.dupont@mail.fr") String email,
        Role role) {
}
