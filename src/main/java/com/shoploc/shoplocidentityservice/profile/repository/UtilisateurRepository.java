package com.shoploc.shoplocidentityservice.profile.repository;

import com.shoploc.shoplocidentityservice.profile.entity.UtilisateurEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface UtilisateurRepository extends JpaRepository<UtilisateurEntity, UUID> {
    Optional<UtilisateurEntity> findByKeycloakId(String keycloakId);
    Optional<UtilisateurEntity> findByEmail(String email);
}
