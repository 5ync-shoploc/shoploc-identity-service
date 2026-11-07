package com.shoploc.shoplocidentityservice.keycloak;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("Utilisateur introuvable : " + userId);
    }
}
