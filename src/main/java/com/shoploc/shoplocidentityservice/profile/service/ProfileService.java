package com.shoploc.shoplocidentityservice.profile.service;

import com.shoploc.shoplocidentityservice.profile.dto.ProfileResponse;
import com.shoploc.shoplocidentityservice.profile.UpdateProfileRequest;
import com.shoploc.shoplocidentityservice.user.Role;
public interface ProfileService {
    ProfileResponse getProfile(String keycloakId, Role tokenRole);
    ProfileResponse updateProfile(String keycloakId, Role tokenRole, UpdateProfileRequest request);
}
