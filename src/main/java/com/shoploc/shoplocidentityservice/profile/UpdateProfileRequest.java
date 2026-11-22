package com.shoploc.shoplocidentityservice.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Champs modifiables du profil : nom, prénom, e-mail et téléphone (facultatif).
 */
public record UpdateProfileRequest(
        @Schema(example = "Dupont") @NotBlank @Size(max = 100) String lastName,
        @Schema(example = "Julie") @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "julie.dupont@mail.fr") @NotBlank @Email @Size(max = 255) String email,

        @Schema(description = "Facultatif : chiffres, espaces, '.', '-' et '+' (6 à 20 caractères) ; vide = supprimé",
                example = "06 12 34 56 78", nullable = true)
        @Pattern(regexp = "^\\+?[0-9 .-]{6,20}$", message = "numéro de téléphone invalide") String phone) {
}
