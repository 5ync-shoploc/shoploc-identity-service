package com.shoploc.shoplocidentityservice.profile.controller;

import com.shoploc.shoplocidentityservice.profile.dto.ProfileResponse;
import com.shoploc.shoplocidentityservice.profile.UpdateProfileRequest;
import com.shoploc.shoplocidentityservice.profile.service.ProfileService;
import com.shoploc.shoplocidentityservice.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * US 1.3 : consulter et modifier MON profil.
 * Pas d'identifiant dans l'URL : on utilise le "sub" du JWT, donc chacun
 * ne peut lire et modifier que son propre profil.
 */
@RestController
@RequestMapping("/api/identity/users/me")
@Tag(name = "Profil", description = "Profil de l'utilisateur connecté. Token Bearer obligatoire.")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Consulter mon profil")
    @ApiResponse(responseCode = "200", description = "Profil de l'utilisateur authentifié")
    @ApiResponse(responseCode = "401", description = "Token absent, expiré ou invalide")
    @ApiResponse(responseCode = "404", description = "Le compte du token n'existe plus")
    @GetMapping
    public ProfileResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getProfile(jwt.getSubject(), roleOf(jwt));
    }

    @Operation(summary = "Modifier mon profil", description = "Nom, prénom, e-mail et téléphone (facultatif).")
    @ApiResponse(responseCode = "200", description = "Profil mis à jour")
    @ApiResponse(responseCode = "400", description = "Champ manquant ou invalide")
    @ApiResponse(responseCode = "401", description = "Token absent, expiré ou invalide")
    @ApiResponse(responseCode = "404", description = "Le compte du token n'existe plus")
    @ApiResponse(responseCode = "409", description = "Le nouvel e-mail appartient déjà à un autre compte")
    @PutMapping
    public ProfileResponse updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(jwt.getSubject(), roleOf(jwt), request);
    }

    /** Rôle ShopLoc lu dans le claim Keycloak {@code realm_access.roles}. */
    public static Role roleOf(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return null;
        }
        return Arrays.stream(Role.values())
                .filter(role -> roles.contains(role.name()))
                .findFirst()
                .orElse(null);
    }
}
