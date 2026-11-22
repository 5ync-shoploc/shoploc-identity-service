package com.shoploc.shoplocidentityservice.profile.dto;

import com.shoploc.shoplocidentityservice.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Profil de l'utilisateur connecté (US 1.3).
 */
public record ProfileResponse(
        @Schema(description = "Identifiant Keycloak (sub du JWT)",
                example = "56718d2d-f5ec-45f3-beeb-c08d8d7951b8")
        String id,

        @Schema(example = "Dupont") String lastName,
        @Schema(example = "Julie") String firstName,
        @Schema(example = "julie.dupont@mail.fr") String email,

        @Schema(description = "Peut être absent (null)", example = "06 12 34 56 78", nullable = true)
        String phone,

        Role role) {
}
