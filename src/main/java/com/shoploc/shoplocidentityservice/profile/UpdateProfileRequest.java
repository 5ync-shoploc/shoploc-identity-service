package com.shoploc.shoplocidentityservice.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Champs modifiables du profil : nom, prénom, e-mail et téléphone (facultatif).
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Email @Size(max = 255) String email,
        @Pattern(regexp = "^\\+?[0-9 .-]{6,20}$", message = "numéro de téléphone invalide") String phone) {
}
