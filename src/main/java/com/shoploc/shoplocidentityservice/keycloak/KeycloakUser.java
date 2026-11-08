package com.shoploc.shoplocidentityservice.keycloak;

/**
 * Profil d'un utilisateur tel que stocké dans Keycloak.
 */
public record KeycloakUser(String id, String email, String firstName, String lastName, String phone) {
}
