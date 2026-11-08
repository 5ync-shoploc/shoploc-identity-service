package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.keycloak.KeycloakTokenClient;
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
public class AuthController {

    private final RegistrationService registrationService;
    private final KeycloakTokenClient tokenClient;

    public AuthController(RegistrationService registrationService, KeycloakTokenClient tokenClient) {
        this.registrationService = registrationService;
        this.tokenClient = tokenClient;
    }

    /** US 1.1 : créer un compte client, commerçant ou collectivité. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

    /** US 1.2 : se connecter et obtenir un token JWT (401 si identifiants invalides). */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return tokenClient.login(request.email().trim().toLowerCase(Locale.ROOT), request.password());
    }
}
