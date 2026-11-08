package com.shoploc.shoplocidentityservice.keycloak;

import com.shoploc.shoplocidentityservice.user.Role;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.HashMap;
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

    /**
     * Lit le profil d'un utilisateur.
     *
     * @throws UserNotFoundException si l'utilisateur n'existe pas (HTTP 404 de Keycloak)
     */
    public KeycloakUser getUser(String userId) {
        Map<String, Object> user = fetchUser(userId);
        return new KeycloakUser(
                (String) user.get("id"),
                (String) user.get("email"),
                (String) user.get("firstName"),
                (String) user.get("lastName"),
                phoneOf(user));
    }

    /**
     * Met à jour nom, prénom, e-mail et téléphone.
     * On relit d'abord la représentation complète puis on la renvoie modifiée,
     * pour ne pas effacer les autres champs du compte.
     *
     * @throws EmailAlreadyUsedException si le nouvel e-mail appartient à un autre compte (HTTP 409)
     */
    public void updateUser(String userId, String email, String firstName, String lastName, String phone) {
        Map<String, Object> user = new HashMap<>(fetchUser(userId));
        user.put("email", email);
        user.put("firstName", firstName);
        user.put("lastName", lastName);

        Map<String, Object> attributes = new HashMap<>();
        if (user.get("attributes") instanceof Map<?, ?> existing) {
            existing.forEach((k, v) -> attributes.put((String) k, v));
        }
        if (phone == null || phone.isBlank()) {
            attributes.remove("phone");
        } else {
            attributes.put("phone", List.of(phone));
        }
        user.put("attributes", attributes);

        restClient.put()
                .uri("/users/{id}", userId)
                .body(user)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.CONFLICT.value(), (request, response) -> {
                    throw new EmailAlreadyUsedException(email);
                })
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUser(String userId) {
        Map<String, Object> user = restClient.get()
                .uri("/users/{id}", userId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                    throw new UserNotFoundException(userId);
                })
                .body(Map.class);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        return user;
    }

    private static String phoneOf(Map<String, Object> user) {
        if (user.get("attributes") instanceof Map<?, ?> attributes
                && attributes.get("phone") instanceof List<?> values
                && !values.isEmpty()) {
            return (String) values.getFirst();
        }
        return null;
    }

    public void deleteUser(String userId) {
        restClient.delete()
                .uri("/users/{id}", userId)
                .retrieve()
                .toBodilessEntity();
    }
}
