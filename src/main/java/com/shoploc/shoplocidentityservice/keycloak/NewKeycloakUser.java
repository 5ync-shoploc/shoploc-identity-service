package com.shoploc.shoplocidentityservice.keycloak;

/**
 * Utilisateur à créer dans Keycloak. Le mot de passe est transmis en clair à
 * Keycloak (via HTTPS en production) qui se charge de le hacher et de le stocker.
 */
public record NewKeycloakUser(String email, String firstName, String lastName, String password) {

    @Override
    public String toString() {
        return "NewKeycloakUser[email=" + email + "]";
    }
}
