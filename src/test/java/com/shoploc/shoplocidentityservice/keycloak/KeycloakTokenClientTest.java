package com.shoploc.shoplocidentityservice.keycloak;

import com.shoploc.shoplocidentityservice.auth.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakTokenClientTest {

    private static final String TOKEN_URI = "http://keycloak/realms/shoploc/protocol/openid-connect/token";

    private MockRestServiceServer keycloak;
    private KeycloakTokenClient client;

    @BeforeEach
    void setUp() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("shoploc-identity")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(TOKEN_URI)
                .build();

        RestClient.Builder builder = RestClient.builder();
        keycloak = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakTokenClient(builder, new InMemoryClientRegistrationRepository(registration));
    }

    @Test
    void login_envoie_les_identifiants_et_renvoie_le_token() {
        keycloak.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "grant_type", "password",
                        "client_id", "shoploc-identity",
                        "client_secret", "secret",
                        "username", "julie@mail.fr",
                        "password", "motdepasse")))
                .andRespond(withSuccess("""
                        {"access_token":"jwt","refresh_token":"refresh","token_type":"Bearer","expires_in":900}""",
                        MediaType.APPLICATION_JSON));

        TokenResponse token = client.login("julie@mail.fr", "motdepasse");

        assertThat(token).isEqualTo(new TokenResponse("jwt", "refresh", "Bearer", 900));
        keycloak.verify();
    }

    @Test
    void login_401_de_keycloak_devient_InvalidCredentialsException() {
        keycloak.expect(requestTo(TOKEN_URI)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.login("julie@mail.fr", "mauvais"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_400_de_keycloak_devient_InvalidCredentialsException() {
        keycloak.expect(requestTo(TOKEN_URI)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.login("julie@mail.fr", "motdepasse"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
