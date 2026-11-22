package com.shoploc.shoplocidentityservice.profile.entity;

import com.shoploc.shoplocidentityservice.user.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "utilisateur", schema = "identity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_utilisateur_keycloak_id", columnNames = "keycloak_id"),
        @UniqueConstraint(name = "uk_utilisateur_email", columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilisateurEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_utilisateur", nullable = false, updatable = false)
    private UUID id;


    @Column(name = "keycloak_id", nullable = false, length = 64)
    private String keycloakId;
    @Column(name = "nom", nullable = false, length = 100)
    private String lastName;
    @Column(name = "prenom", nullable = false, length = 100)
    private String firstName;
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    @Column(name = "telephone", length = 20)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    public void updateProfile(String lastName, String firstName, String email, String phone) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.email = email;
        this.phone = phone;
    }
}
