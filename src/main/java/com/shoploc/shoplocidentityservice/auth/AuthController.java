package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.keycloak.KeycloakTokenClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/identity/auth")
@Tag(name = "Auth", description = "Inscription et connexion. Accès public : pas de token requis.")
public class AuthController {

    private final RegistrationService registrationService;
    private final KeycloakTokenClient tokenClient;

    public AuthController(RegistrationService registrationService, KeycloakTokenClient tokenClient) {
        this.registrationService = registrationService;
        this.tokenClient = tokenClient;
    }

    /** US 1.1 : créer un compte client, commerçant ou collectivité. */
    @Operation(summary = "Créer un compte",
            description = "Keycloak hache et stocke le mot de passe ; le rôle est attribué à l'inscription.")
    @ApiResponse(responseCode = "201", description = "Compte créé")
    @ApiResponse(responseCode = "400", description = "Champ manquant ou invalide")
    @ApiResponse(responseCode = "409", description = "Un compte existe déjà avec cet e-mail")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

    /** US 1.2 : se connecter et obtenir un token JWT (401 si identifiants invalides). */
    @Operation(summary = "Se connecter",
            description = "Renvoie un access token (JWT, 15 min) et un refresh token (OpenID Connect / Keycloak).")
    @ApiResponse(responseCode = "200", description = "Connecté")
    @ApiResponse(responseCode = "400", description = "E-mail ou mot de passe manquant")
    @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return tokenClient.login(request.email().trim().toLowerCase(Locale.ROOT), request.password());
    }
}
