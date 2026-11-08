package com.shoploc.shoplocidentityservice.profile;

import com.shoploc.shoplocidentityservice.keycloak.KeycloakAdminClient;
import com.shoploc.shoplocidentityservice.keycloak.KeycloakUser;
import com.shoploc.shoplocidentityservice.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    KeycloakAdminClient keycloak;

    @InjectMocks
    ProfileService service;

    @Test
    void lit_le_profil_dans_keycloak() {
        when(keycloak.getUser("user-123"))
                .thenReturn(new KeycloakUser("user-123", "julie@mail.fr", "Julie", "Dupont", "0612345678"));

        assertThat(service.getProfile("user-123", Role.CLIENT)).isEqualTo(
                new ProfileResponse("user-123", "Dupont", "Julie", "julie@mail.fr", "0612345678", Role.CLIENT));
    }

    @Test
    void modifie_le_profil_avec_email_normalise() {
        when(keycloak.getUser("user-123"))
                .thenReturn(new KeycloakUser("user-123", "julie.d@mail.fr", "Julie", "Durand", "0612345678"));

        ProfileResponse response = service.updateProfile("user-123", Role.CLIENT,
                new UpdateProfileRequest(" Durand ", "Julie", " Julie.D@Mail.fr ", " 0612345678 "));

        verify(keycloak).updateUser("user-123", "julie.d@mail.fr", "Julie", "Durand", "0612345678");
        assertThat(response.lastName()).isEqualTo("Durand");
    }

    @Test
    void telephone_vide_est_supprime() {
        when(keycloak.getUser("user-123"))
                .thenReturn(new KeycloakUser("user-123", "julie@mail.fr", "Julie", "Dupont", null));

        service.updateProfile("user-123", Role.CLIENT, new UpdateProfileRequest("Dupont", "Julie", "julie@mail.fr", ""));

        verify(keycloak).updateUser("user-123", "julie@mail.fr", "Julie", "Dupont", null);
    }
}
