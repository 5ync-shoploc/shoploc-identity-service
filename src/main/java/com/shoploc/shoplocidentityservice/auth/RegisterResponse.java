package com.shoploc.shoplocidentityservice.auth;

import com.shoploc.shoplocidentityservice.user.Role;

/**
 * Compte créé, renvoyé au client (sans le mot de passe).
 */
public record RegisterResponse(String id, String lastName, String firstName, String email, Role role) {
}
