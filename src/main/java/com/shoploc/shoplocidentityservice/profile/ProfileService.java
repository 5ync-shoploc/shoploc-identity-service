package com.shoploc.shoplocidentityservice.profile;

import com.shoploc.shoplocidentityservice.keycloak.KeycloakAdminClient;
import com.shoploc.shoplocidentityservice.keycloak.KeycloakUser;
import com.shoploc.shoplocidentityservice.user.Role;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Lecture et modification du profil de l'utilisateur connecté.
 * L'identifiant vient toujours du token : impossible de viser le profil d'un autre.
 */
@Service
public class ProfileService {

    private final KeycloakAdminClient keycloak;

    public ProfileService(KeycloakAdminClient keycloak) {
        this.keycloak = keycloak;
    }

    public ProfileResponse getProfile(String userId, Role role) {
        return toResponse(keycloak.getUser(userId), role);
    }

    public ProfileResponse updateProfile(String userId, Role role, UpdateProfileRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();

        keycloak.updateUser(userId, email, request.firstName().trim(), request.lastName().trim(), phone);
        return toResponse(keycloak.getUser(userId), role);
    }

    private static ProfileResponse toResponse(KeycloakUser user, Role role) {
        return new ProfileResponse(user.id(), user.lastName(), user.firstName(), user.email(), user.phone(), role);
    }
}
