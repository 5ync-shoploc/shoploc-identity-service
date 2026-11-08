package com.shoploc.shoplocidentityservice.profile;

import com.shoploc.shoplocidentityservice.config.SecurityConfig;
import com.shoploc.shoplocidentityservice.keycloak.EmailAlreadyUsedException;
import com.shoploc.shoplocidentityservice.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    private static final String URL = "/api/identity/users/me";
    private static final String BODY = """
            {"lastName":"Dupont","firstName":"Julie","email":"julie@mail.fr","phone":"06 12 34 56 78"}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProfileService profileService;

    private static ProfileResponse julie() {
        return new ProfileResponse("user-123", "Dupont", "Julie", "julie@mail.fr", "06 12 34 56 78", Role.CLIENT);
    }

    @Test
    void sans_token_renvoie_401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(profileService);
    }

    @Test
    void consulter_mon_profil_utilise_le_sub_et_le_role_du_token() throws Exception {
        when(profileService.getProfile("user-123", Role.CLIENT)).thenReturn(julie());

        mvc.perform(get(URL).with(jwt().jwt(j -> j.subject("user-123")
                        .claim("realm_access", Map.of("roles", List.of("default-roles-shoploc", "CLIENT"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.phone").value("06 12 34 56 78"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void modifier_mon_profil_renvoie_200() throws Exception {
        when(profileService.updateProfile(eq("user-123"), eq(Role.CLIENT), any())).thenReturn(julie());

        mvc.perform(put(URL).with(jwt().jwt(j -> j.subject("user-123")
                                .claim("realm_access", Map.of("roles", List.of("CLIENT")))))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("julie@mail.fr"));

        verify(profileService).updateProfile("user-123", Role.CLIENT,
                new UpdateProfileRequest("Dupont", "Julie", "julie@mail.fr", "06 12 34 56 78"));
    }

    @Test
    void telephone_invalide_renvoie_400() throws Exception {
        mvc.perform(put(URL).with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"lastName":"Dupont","firstName":"Julie","email":"julie@mail.fr","phone":"abc"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void email_deja_pris_par_un_autre_compte_renvoie_409() throws Exception {
        when(profileService.updateProfile(any(), any(), any())).thenThrow(new EmailAlreadyUsedException("julie@mail.fr"));

        mvc.perform(put(URL).with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void roleOf_sans_claim_realm_access_renvoie_null() {
        var token = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("t")
                .header("alg", "none").subject("user-123").build();
        org.assertj.core.api.Assertions.assertThat(ProfileController.roleOf(token)).isNull();
    }
}
