package com.shoploc.shoplocidentityservice.profile;

import com.shoploc.shoplocidentityservice.user.Role;

/**
 * Profil de l'utilisateur connecté (US 1.3).
 */
public record ProfileResponse(String id, String lastName, String firstName, String email, String phone, Role role) {
}
