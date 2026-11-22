package com.shoploc.shoplocidentityservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Déclare le contrat d'API (titre affiché dans Swagger UI, et le schéma
 * d'authentification Bearer utilisé par les endpoints protégés).
 * Swagger UI : /swagger-ui.html — JSON du contrat : /v3/api-docs.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "ShopLoc – Identity Service",
        version = "v1",
        description = "Inscription, connexion et gestion du profil (Keycloak)."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Jeton renvoyé par POST /api/identity/auth/login.")
public class OpenApiConfig {
}
