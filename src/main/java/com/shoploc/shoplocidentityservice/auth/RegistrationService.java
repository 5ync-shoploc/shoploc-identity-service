package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.keycloak.KeycloakAdminClient;
import com.shoploc.shoplocidentityservice.keycloak.NewKeycloakUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Inscription d'un utilisateur : création du compte dans Keycloak puis attribution du rôle.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final KeycloakAdminClient keycloak;

    public RegistrationService(KeycloakAdminClient keycloak) {
        this.keycloak = keycloak;
    }

    public RegisterResponse register(RegisterRequest request) {
        // L'e-mail sert d'identifiant : on le normalise pour garantir l'unicité
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        var user = new NewKeycloakUser(email, request.firstName().trim(), request.lastName().trim(), request.password());
        String userId = keycloak.createUser(user); // EmailAlreadyUsedException si l'e-mail existe déjà

        try {
            keycloak.assignRealmRole(userId, request.role());
        } catch (RuntimeException e) {
            // Pas de compte sans rôle : on annule la création
            log.error("Attribution du rôle {} impossible pour {}, suppression du compte", request.role(), userId, e);
            keycloak.deleteUser(userId);
            throw e;
        }

        log.info("Compte {} créé avec le rôle {}", userId, request.role());
        return new RegisterResponse(userId, user.lastName(), user.firstName(), email, request.role());
    }
}
