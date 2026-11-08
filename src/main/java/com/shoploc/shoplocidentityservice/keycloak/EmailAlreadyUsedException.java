package com.shoploc.shoplocidentityservice.keycloak;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Un compte existe déjà avec l'e-mail " + email);
    }
}
