package com.shoploc.shoplocidentityservice.profile.mapper;

import com.shoploc.shoplocidentityservice.profile.dto.ProfileResponse;
import com.shoploc.shoplocidentityservice.profile.entity.UtilisateurEntity;

public final class UtilisateurMapper {
    private UtilisateurMapper() { }
    public static ProfileResponse toResponse(UtilisateurEntity entity) {
        return new ProfileResponse(entity.getId().toString(), entity.getLastName(), entity.getFirstName(),
                entity.getEmail(), entity.getPhone(), entity.getRole());
    }
}
