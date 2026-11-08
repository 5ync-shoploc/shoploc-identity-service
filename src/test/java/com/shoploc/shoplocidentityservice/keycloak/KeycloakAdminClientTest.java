package com.shoploc.shoplocidentityservice.keycloak;

import com.shoploc.shoplocidentityservice.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Teste les appels REST vers Keycloak sans Keycloak : MockRestServiceServer
 * simule les réponses de l'API d'administration.
 */
class KeycloakAdminClientTest {

    private static final String BASE = "http://keycloak/admin/realms/shoploc";

    private MockRestServiceServer keycloak;
    private KeycloakAdminClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        keycloak = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakAdminClient(builder.build());
    }

    @Test
    void createUser_envoie_le_compte_et_renvoie_l_id_lu_dans_location() {
        keycloak.expect(requestTo(BASE + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.username").value("julie@mail.fr"))
                .andExpect(jsonPath("$.email").value("julie@mail.fr"))
                .andExpect(jsonPath("$.firstName").value("Julie"))
                .andExpect(jsonPath("$.lastName").value("Dupont"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.credentials[0].type").value("password"))
                .andExpect(jsonPath("$.credentials[0].value").value("motdepasse"))
                .andExpect(jsonPath("$.credentials[0].temporary").value(false))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, BASE + "/users/abc-123"));

        String id = client.createUser(new NewKeycloakUser("julie@mail.fr", "Julie", "Dupont", "motdepasse"));

        assertThat(id).isEqualTo("abc-123");
        keycloak.verify();
    }

    @Test
    void createUser_409_devient_EmailAlreadyUsedException() {
        keycloak.expect(requestTo(BASE + "/users"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.createUser(
                new NewKeycloakUser("julie@mail.fr", "Julie", "Dupont", "motdepasse")))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessageContaining("julie@mail.fr");
    }

    @Test
    void createUser_sans_location_est_une_erreur() {
        keycloak.expect(requestTo(BASE + "/users"))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertThatThrownBy(() -> client.createUser(
                new NewKeycloakUser("julie@mail.fr", "Julie", "Dupont", "motdepasse")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void assignRealmRole_lit_le_role_puis_l_attribue() {
        keycloak.expect(requestTo(BASE + "/roles/COMMERCANT"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"role-1\",\"name\":\"COMMERCANT\"}", MediaType.APPLICATION_JSON));
        keycloak.expect(requestTo(BASE + "/users/abc-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].id").value("role-1"))
                .andExpect(jsonPath("$[0].name").value("COMMERCANT"))
                .andRespond(withNoContent());

        client.assignRealmRole("abc-123", Role.COMMERCANT);

        keycloak.verify();
    }

    @Test
    void deleteUser_supprime_le_compte() {
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        client.deleteUser("abc-123");

        keycloak.verify();
    }

    // --- US 1.3 : profil ---

    private static final String USER_JSON = """
            {"id":"abc-123","username":"julie@mail.fr","email":"julie@mail.fr","firstName":"Julie",
             "lastName":"Dupont","enabled":true,"attributes":{"phone":["0612345678"]}}""";

    @Test
    void getUser_lit_le_profil_et_le_telephone() {
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.getUser("abc-123"))
                .isEqualTo(new KeycloakUser("abc-123", "julie@mail.fr", "Julie", "Dupont", "0612345678"));
    }

    @Test
    void getUser_sans_telephone() {
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andRespond(withSuccess("{\"id\":\"abc-123\",\"email\":\"julie@mail.fr\"}", MediaType.APPLICATION_JSON));

        assertThat(client.getUser("abc-123").phone()).isNull();
    }

    @Test
    void getUser_404_devient_UserNotFoundException() {
        keycloak.expect(requestTo(BASE + "/users/abc-123")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getUser("abc-123")).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUser_relit_le_compte_puis_renvoie_la_version_modifiee() {
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.email").value("julie.d@mail.fr"))
                .andExpect(jsonPath("$.lastName").value("Durand"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.attributes.phone[0]").value("0700000000"))
                .andRespond(withNoContent());

        client.updateUser("abc-123", "julie.d@mail.fr", "Julie", "Durand", "0700000000");

        keycloak.verify();
    }

    @Test
    void updateUser_sans_telephone_retire_l_attribut() {
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.attributes.phone").doesNotExist())
                .andRespond(withNoContent());

        client.updateUser("abc-123", "julie@mail.fr", "Julie", "Dupont", null);

        keycloak.verify();
    }

    @Test
    void updateUser_409_devient_EmailAlreadyUsedException() {
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));
        keycloak.expect(requestTo(BASE + "/users/abc-123"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.updateUser("abc-123", "pris@mail.fr", "Julie", "Dupont", null))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }
}
