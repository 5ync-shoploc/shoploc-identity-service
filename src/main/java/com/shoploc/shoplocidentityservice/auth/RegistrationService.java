package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.keycloak.KeycloakAdminClient;
import com.shoploc.shoplocidentityservice.keycloak.NewKeycloakUser;
import com.shoploc.shoplocidentityservice.profile.entity.UtilisateurEntity;
import com.shoploc.shoplocidentityservice.profile.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Inscription d'un utilisateur : création du compte dans Keycloak, attribution du
 * rôle, puis copie locale du profil (table {@code identity.utilisateur}) lue par
 * GET/PUT /users/me sans rappeler Keycloak à chaque fois.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final KeycloakAdminClient keycloak;
    private final UtilisateurRepository utilisateurRepository;

    public RegistrationService(KeycloakAdminClient keycloak, UtilisateurRepository utilisateurRepository) {
        this.keycloak = keycloak;
        this.utilisateurRepository = utilisateurRepository;
    }

    public RegisterResponse register(RegisterRequest request) {
        // L'e-mail sert d'identifiant : on le normalise pour garantir l'unicité
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String lastName = request.lastName().trim();
        String firstName = request.firstName().trim();

        var user = new NewKeycloakUser(email, firstName, lastName, request.password());
        String keycloakId = keycloak.createUser(user); // EmailAlreadyUsedException si l'e-mail existe déjà

        try {
            keycloak.assignRealmRole(keycloakId, request.role());
        } catch (RuntimeException e) {
            // Pas de compte sans rôle : on annule la création
            log.error("Attribution du rôle {} impossible pour {}, suppression du compte", request.role(), keycloakId, e);
            keycloak.deleteUser(keycloakId);
            throw e;
        }

        // Copie locale du profil : sans elle, GET/PUT /users/me échoue juste après l'inscription.
        utilisateurRepository.save(UtilisateurEntity.builder()
                .keycloakId(keycloakId)
                .lastName(lastName)
                .firstName(firstName)
                .email(email)
                .role(request.role())
                .build());

        log.info("Compte {} créé avec le rôle {}", keycloakId, request.role());
        return new RegisterResponse(keycloakId, lastName, firstName, email, request.role());
    }
}
