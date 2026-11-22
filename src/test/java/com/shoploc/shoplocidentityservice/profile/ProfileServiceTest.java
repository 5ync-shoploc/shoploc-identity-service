package com.shoploc.shoplocidentityservice.profile;

import com.shoploc.shoplocidentityservice.keycloak.KeycloakAdminClient;
import com.shoploc.shoplocidentityservice.keycloak.UserNotFoundException;
import com.shoploc.shoplocidentityservice.profile.dto.ProfileResponse;
import com.shoploc.shoplocidentityservice.profile.entity.UtilisateurEntity;
import com.shoploc.shoplocidentityservice.profile.repository.UtilisateurRepository;
import com.shoploc.shoplocidentityservice.profile.service.ProfileServiceImpl;
import com.shoploc.shoplocidentityservice.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final UUID ID = UUID.fromString("56718d2d-f5ec-45f3-beeb-c08d8d7951b8");

    @Mock
    UtilisateurRepository repository;

    @Mock
    KeycloakAdminClient keycloak;

    @InjectMocks
    ProfileServiceImpl service;

    private static UtilisateurEntity julie(String phone) {
        return UtilisateurEntity.builder()
                .id(ID)
                .keycloakId("user-123")
                .lastName("Dupont")
                .firstName("Julie")
                .email("julie@mail.fr")
                .phone(phone)
                .role(Role.CLIENT)
                .build();
    }

    @Test
    void lit_le_profil_en_base() {
        when(repository.findByKeycloakId("user-123")).thenReturn(Optional.of(julie("0612345678")));

        assertThat(service.getProfile("user-123", Role.CLIENT)).isEqualTo(
                new ProfileResponse(ID.toString(), "Dupont", "Julie", "julie@mail.fr", "0612345678", Role.CLIENT));
    }

    @Test
    void profil_introuvable_leve_UserNotFoundException() {
        when(repository.findByKeycloakId("inconnu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile("inconnu", Role.CLIENT))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void modifie_le_profil_avec_email_normalise() {
        UtilisateurEntity entity = julie("0612345678");
        when(repository.findByKeycloakId("user-123")).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        ProfileResponse response = service.updateProfile("user-123", Role.CLIENT,
                new UpdateProfileRequest(" Durand ", "Julie", " Julie.D@Mail.fr ", " 0612345678 "));

        verify(keycloak).updateUser("user-123", "julie.d@mail.fr", "Julie", "Durand", "0612345678");
        assertThat(response.lastName()).isEqualTo("Durand");
    }

    @Test
    void telephone_vide_est_supprime() {
        UtilisateurEntity entity = julie("0612345678");
        when(repository.findByKeycloakId("user-123")).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        service.updateProfile("user-123", Role.CLIENT, new UpdateProfileRequest("Dupont", "Julie", "julie@mail.fr", ""));

        verify(keycloak).updateUser("user-123", "julie@mail.fr", "Julie", "Dupont", null);
    }
}
