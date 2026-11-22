package com.shoploc.shoplocidentityservice.profile.service;

import com.shoploc.shoplocidentityservice.keycloak.KeycloakAdminClient;
import com.shoploc.shoplocidentityservice.keycloak.UserNotFoundException;
import com.shoploc.shoplocidentityservice.profile.dto.ProfileResponse;
import com.shoploc.shoplocidentityservice.profile.UpdateProfileRequest;
import com.shoploc.shoplocidentityservice.profile.entity.UtilisateurEntity;
import com.shoploc.shoplocidentityservice.profile.mapper.UtilisateurMapper;
import com.shoploc.shoplocidentityservice.profile.repository.UtilisateurRepository;
import com.shoploc.shoplocidentityservice.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;


@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {
    private final UtilisateurRepository repository;
    private final KeycloakAdminClient keycloak;

    public ProfileServiceImpl(UtilisateurRepository repository, KeycloakAdminClient keycloak) {
        this.repository = repository;
        this.keycloak = keycloak;
    }


    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String keycloakId, Role tokenRole) {
        UtilisateurEntity entity = repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));
        return UtilisateurMapper.toResponse(entity);
    }

    @Override
    public ProfileResponse updateProfile(String keycloakId, Role tokenRole, UpdateProfileRequest request) {
        UtilisateurEntity entity = repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();

        // Email, nom et prénom restent synchronisés avec l'identité Keycloak.
        keycloak.updateUser(keycloakId, email, firstName, lastName, phone);
        entity.updateProfile(lastName, firstName, email, phone);
        return UtilisateurMapper.toResponse(repository.save(entity));
    }
}
