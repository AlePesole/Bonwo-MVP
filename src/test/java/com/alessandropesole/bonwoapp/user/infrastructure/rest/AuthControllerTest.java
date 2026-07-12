package com.alessandropesole.bonwoapp.user.infrastructure.rest;

import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthEntryPoint;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthenticationFilter;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtService;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.SecurityConfig;
import com.alessandropesole.bonwoapp.user.application.dto.AuthRequest;
import com.alessandropesole.bonwoapp.user.application.dto.AuthResponse;
import com.alessandropesole.bonwoapp.user.application.dto.RegisterRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;
import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import com.alessandropesole.bonwoapp.user.domain.port.in.AuthUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthEntryPoint.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse sampleUser() {
        return new UserResponse(1L, "user@example.com", "johndoe", UserRole.USER,
                AccountStatus.ACTIVE, Instant.now());
    }

    @Test
    void register_withValidBody_isCreated() throws Exception {
        when(authUseCase.register(any())).thenReturn(sampleUser());

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("user@example.com", "password123", "johndoe"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void register_withInvalidEmail_isBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("not-an-email", "password123", "johndoe"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortPassword_isBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("user@example.com", "short", "johndoe"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidBody_isOk() throws Exception {
        AuthResponse response = AuthResponse.of("access", "refresh", sampleUser());
        when(authUseCase.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("user@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_withBlankPassword_isBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("user@example.com", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withValidToken_isOk() throws Exception {
        AuthResponse response = AuthResponse.of("new-access", "new-refresh", sampleUser());
        when(authUseCase.refreshToken("old-token")).thenReturn(response);

        mockMvc.perform(post("/auth/refresh").param("refreshToken", "old-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void logout_isNoContent() throws Exception {
        mockMvc.perform(post("/auth/logout").param("refreshToken", "some-token"))
                .andExpect(status().isNoContent());
    }
}
