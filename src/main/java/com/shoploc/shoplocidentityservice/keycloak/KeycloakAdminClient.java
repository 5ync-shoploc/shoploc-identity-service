package com.shoploc.shoplocidentityservice.keycloak;

import com.shoploc.shoplocidentityservice.user.Role;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Appels à l'API REST d'administration de Keycloak (realm ShopLoc).
 * Documentation : https://www.keycloak.org/docs-api/latest/rest-api/
 */
@Component
public class KeycloakAdminClient {

    private final RestClient restClient;

    public KeycloakAdminClient(@Qualifier("keycloakAdminRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Crée l'utilisateur et renvoie son identifiant Keycloak.
     *
     * @throws EmailAlreadyUsedException si un compte existe déjà avec cet e-mail (HTTP 409 de Keycloak)
     */
    public String createUser(NewKeycloakUser user) {
        Map<String, Object> body = Map.of(
                "username", user.email(),
                "email", user.email(),
                "firstName", user.firstName(),
                "lastName", user.lastName(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", user.password(),
                        "temporary", false)));

        URI location = restClient.post()
                .uri("/users")
                .body(body)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.CONFLICT.value(), (request, response) -> {
                    throw new EmailAlreadyUsedException(user.email());
                })
                .toBodilessEntity()
                .getHeaders()
                .getLocation();

        if (location == null) {
            throw new IllegalStateException("Keycloak n'a pas renvoyé l'URL du compte créé");
        }
        // Location = .../admin/realms/shoploc/users/{id}
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Donne un rôle du realm (CLIENT, COMMERCANT, COLLECTIVITE) à l'utilisateur. */
    public void assignRealmRole(String userId, Role role) {
        // Keycloak attend la représentation complète du rôle (avec son id)
        Map<?, ?> roleRepresentation = restClient.get()
                .uri("/roles/{name}", role.name())
                .retrieve()
                .body(Map.class);

        restClient.post()
                .uri("/users/{id}/role-mappings/realm", userId)
                .body(List.of(roleRepresentation))
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteUser(String userId) {
        restClient.delete()
                .uri("/users/{id}", userId)
                .retrieve()
                .toBodilessEntity();
    }
}
