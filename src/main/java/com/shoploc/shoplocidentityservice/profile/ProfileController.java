package com.shoploc.shoplocidentityservice.profile;

import com.shoploc.shoplocidentityservice.user.Role;
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
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getProfile(jwt.getSubject(), roleOf(jwt));
    }

    @PutMapping
    public ProfileResponse updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(jwt.getSubject(), roleOf(jwt), request);
    }

    /** Rôle ShopLoc lu dans le claim Keycloak {@code realm_access.roles}. */
    static Role roleOf(Jwt jwt) {
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
