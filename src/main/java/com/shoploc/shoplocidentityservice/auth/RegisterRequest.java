package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Données envoyées par le visiteur pour créer son compte (US 1.1).
 */
public record RegisterRequest(
        @Schema(description = "Nom de famille", example = "Dupont")
        @NotBlank @Size(max = 100) String lastName,

        @Schema(description = "Prénom", example = "Julie")
        @NotBlank @Size(max = 100) String firstName,

        @Schema(description = "E-mail, unique, sert d'identifiant de connexion", example = "julie.dupont@mail.fr")
        @NotBlank @Email @Size(max = 255) String email,

        @Schema(description = "8 caractères minimum ; haché et stocké par Keycloak, jamais renvoyé",
                example = "motdepasse")
        @NotBlank @Size(min = 8, max = 128) String password,

        @Schema(description = "Rôle attribué à l'inscription", example = "CLIENT")
        @NotNull Role role) {

    @Override
    public String toString() {
        // Ne jamais écrire le mot de passe dans les logs
        return "RegisterRequest[email=" + email + ", role=" + role + "]";
    }
}
