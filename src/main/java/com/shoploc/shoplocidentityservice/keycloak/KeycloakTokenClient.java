package com.shoploc.shoplocidentityservice.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shoploc.shoplocidentityservice.auth.TokenResponse;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Demande un token à Keycloak (endpoint OpenID Connect {@code /token}) avec
 * l'e-mail et le mot de passe de l'utilisateur.
 * <p>
 * Le client {@code shoploc-identity} et son secret sont les mêmes que pour
 * l'API d'administration : on les relit dans la configuration Spring Security.
 */
@Component
public class KeycloakTokenClient {

    private final RestClient restClient;
    private final ClientRegistration registration;

    public KeycloakTokenClient(RestClient.Builder builder, ClientRegistrationRepository registrations) {
        this.registration = registrations.findByRegistrationId("keycloak");
        this.restClient = builder.build();
    }

    /**
     * @throws InvalidCredentialsException si Keycloak refuse les identifiants (HTTP 400/401)
     */
    public TokenResponse login(String email, String password) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", registration.getClientId());
        form.add("client_secret", registration.getClientSecret());
        form.add("username", email);
        form.add("password", password);
        form.add("scope", "openid");

        KeycloakToken token = restClient.post()
                .uri(registration.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                // Keycloak répond 401 (mauvais mot de passe) ou 400 (compte désactivé, etc.)
                .onStatus(status -> status.value() == 400 || status.value() == 401, (request, response) -> {
                    throw new InvalidCredentialsException();
                })
                .body(KeycloakToken.class);

        if (token == null) {
            throw new IllegalStateException("Réponse vide de Keycloak");
        }
        return new TokenResponse(token.accessToken(), token.refreshToken(), token.tokenType(), token.expiresIn());
    }

    /** Format de la réponse de Keycloak (noms en snake_case). */
    record KeycloakToken(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn) {
    }
}
