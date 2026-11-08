package com.shoploc.shoplocidentityservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Client HTTP vers l'API d'administration de Keycloak.
 * <p>
 * Chaque requête reçoit automatiquement un token technique obtenu en
 * "client credentials" avec le client {@code shoploc-identity}. Le token est
 * mis en cache et renouvelé par Spring Security quand il expire.
 */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakClientConfig {

    static final String REGISTRATION_ID = "keycloak";

    @Bean
    RestClient keycloakAdminRestClient(RestClient.Builder builder,
                                       KeycloakProperties properties,
                                       ClientRegistrationRepository registrations,
                                       OAuth2AuthorizedClientService authorizedClients) {

        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClients);

        var oauth2 = new OAuth2ClientHttpRequestInterceptor(manager);
        oauth2.setClientRegistrationIdResolver(request -> REGISTRATION_ID);
        // Le token appartient au service lui-même, pas à l'utilisateur qui fait la requête.
        oauth2.setPrincipalResolver(request ->
                UsernamePasswordAuthenticationToken.unauthenticated("shoploc-identity-service", null));

        return builder
                .baseUrl(properties.adminBaseUrl())
                .requestInterceptor(oauth2)
                .build();
    }
}
