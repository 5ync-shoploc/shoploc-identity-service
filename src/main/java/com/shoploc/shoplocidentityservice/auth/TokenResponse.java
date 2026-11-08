package com.shoploc.shoplocidentityservice.auth;

/**
 * Tokens délivrés par Keycloak après une connexion réussie.
 * L'access token (JWT) contient l'identifiant de l'utilisateur ({@code sub})
 * et ses rôles ({@code realm_access.roles}).
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {
}
