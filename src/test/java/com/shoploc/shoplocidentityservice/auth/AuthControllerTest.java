package com.shoploc.shoplocidentityservice.auth;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String URL = "/api/identity/auth/register";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RegistrationService registrationService;

    @Test
    void inscription_valide_renvoie_201_sans_authentification() throws Exception {
        when(registrationService.register(any())).thenReturn(
                new RegisterResponse("user-123", "Dupont", "Julie", "julie@mail.fr", Role.CLIENT));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"lastName":"Dupont","firstName":"Julie","email":"julie@mail.fr",
                         "password":"motdepasse","role":"CLIENT"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void champs_obligatoires_manquants_renvoie_400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"pas-un-email","password":"court"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.role").exists());

        verifyNoInteractions(registrationService);
    }

    @Test
    void role_inconnu_renvoie_400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"lastName":"Dupont","firstName":"Julie","email":"julie@mail.fr",
                         "password":"motdepasse","role":"ADMIN"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void email_deja_utilise_renvoie_409() throws Exception {
        when(registrationService.register(any())).thenThrow(new EmailAlreadyUsedException("julie@mail.fr"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"lastName":"Dupont","firstName":"Julie","email":"julie@mail.fr",
                         "password":"motdepasse","role":"CLIENT"}"""))
                .andExpect(status().isConflict());
    }
}
