package com.shoploc.shoplocidentityservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Adresse du serveur Keycloak et nom du realm ShopLoc (section {@code shoploc.keycloak}).
 */
@ConfigurationProperties(prefix = "shoploc.keycloak")
public record KeycloakProperties(String serverUrl, String realm) {

    /** URL de base de l'API d'administration du realm, ex. http://localhost:8180/admin/realms/shoploc */
    public String adminBaseUrl() {
        return serverUrl + "/admin/realms/" + realm;
    }
}
