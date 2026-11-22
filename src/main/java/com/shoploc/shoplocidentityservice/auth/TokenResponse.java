package com.shoploc.shoplocidentityservice.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tokens délivrés par Keycloak après une connexion réussie.
 * L'access token (JWT) contient l'identifiant de l'utilisateur ({@code sub})
 * et ses rôles ({@code realm_access.roles}).
 */
public record TokenResponse(
        @Schema(description = "JWT à envoyer dans l'en-tête Authorization: Bearer <accessToken>, valable 15 min")
        String accessToken,

        @Schema(description = "Permet d'obtenir un nouvel accessToken sans redemander le mot de passe")
        String refreshToken,

        @Schema(example = "Bearer") String tokenType,

        @Schema(description = "Durée de validité de l'accessToken, en secondes", example = "900")
        long expiresIn) {
}
