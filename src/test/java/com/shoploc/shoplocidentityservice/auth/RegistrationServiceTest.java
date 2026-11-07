package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.keycloak.EmailAlreadyUsedException;
import com.shoploc.shoplocidentityservice.keycloak.KeycloakAdminClient;
import com.shoploc.shoplocidentityservice.keycloak.NewKeycloakUser;
import com.shoploc.shoplocidentityservice.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    KeycloakAdminClient keycloak;

    @InjectMocks
    RegistrationService service;

    @Test
    void cree_le_compte_puis_attribue_le_role() {
        when(keycloak.createUser(any())).thenReturn("user-123");

        RegisterResponse response = service.register(
                new RegisterRequest("Dupont", "Julie", "  Julie@Mail.FR ", "motdepasse", Role.CLIENT));

        verify(keycloak).createUser(new NewKeycloakUser("julie@mail.fr", "Julie", "Dupont", "motdepasse"));
        verify(keycloak).assignRealmRole("user-123", Role.CLIENT);
        assertThat(response).isEqualTo(
                new RegisterResponse("user-123", "Dupont", "Julie", "julie@mail.fr", Role.CLIENT));
    }

    @Test
    void email_deja_utilise_aucun_role_attribue() {
        when(keycloak.createUser(any())).thenThrow(new EmailAlreadyUsedException("julie@mail.fr"));

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Dupont", "Julie", "julie@mail.fr", "motdepasse", Role.CLIENT)))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(keycloak, never()).assignRealmRole(any(), any());
    }

    @Test
    void supprime_le_compte_si_le_role_ne_peut_pas_etre_attribue() {
        when(keycloak.createUser(any())).thenReturn("user-123");
        doThrow(new IllegalStateException("Keycloak indisponible"))
                .when(keycloak).assignRealmRole("user-123", Role.COMMERCANT);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Martin", "Suzanne", "suzanne@mail.fr", "motdepasse", Role.COMMERCANT)))
                .isInstanceOf(IllegalStateException.class);

        verify(keycloak).deleteUser("user-123");
    }
}
