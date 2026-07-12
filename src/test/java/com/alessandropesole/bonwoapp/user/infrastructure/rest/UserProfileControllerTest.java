package com.alessandropesole.bonwoapp.user.infrastructure.rest;

import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthEntryPoint;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthenticationFilter;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtService;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.SecurityConfig;
import com.alessandropesole.bonwoapp.user.application.dto.UpdateProfileRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserProfileResponse;
import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
import com.alessandropesole.bonwoapp.user.domain.port.in.UserProfileUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthEntryPoint.class})
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileUseCase profileUseCase;
    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserProfileResponse sampleProfile() {
        return new UserProfileResponse(1L, "johndoe", null, "bio", 30, 180, 80.0, java.util.List.of());
    }

    @Test
    void getMyProfile_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getMyProfile_withAuthenticatedUser_isOk() throws Exception {
        when(currentUserResolver.resolveId(any())).thenReturn(1L);
        when(profileUseCase.getMyProfile(1L)).thenReturn(sampleProfile());

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void getPublicProfile_withoutAuthentication_isOk() throws Exception {
        when(profileUseCase.getPublicProfile("johndoe")).thenReturn(sampleProfile());

        mockMvc.perform(get("/users/johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    @WithMockUser
    void getPublicProfile_withAuthenticatedUser_isOk() throws Exception {
        when(profileUseCase.getPublicProfile("johndoe")).thenReturn(sampleProfile());

        mockMvc.perform(get("/users/johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void updateProfile_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequest(null, "bio", null, null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void updateProfile_withAuthenticatedUser_isOk() throws Exception {
        when(currentUserResolver.resolveId(any())).thenReturn(1L);
        when(profileUseCase.updateProfile(eq(1L), any())).thenReturn(sampleProfile());

        mockMvc.perform(patch("/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequest(null, "new bio", null, null, null, null))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateProfile_withInvalidAge_isBadRequest() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequest(null, null, 5, null, null, null))))
                .andExpect(status().isBadRequest());
    }
}
