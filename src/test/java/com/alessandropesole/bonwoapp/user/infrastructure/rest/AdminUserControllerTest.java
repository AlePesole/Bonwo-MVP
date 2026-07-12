package com.alessandropesole.bonwoapp.user.infrastructure.rest;

import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthEntryPoint;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthenticationFilter;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtService;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.SecurityConfig;
import com.alessandropesole.bonwoapp.user.application.dto.AdminUpdateUserRequest;
import com.alessandropesole.bonwoapp.user.application.dto.ChangeRoleRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;
import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import com.alessandropesole.bonwoapp.user.domain.port.in.UserManagementUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthEntryPoint.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserManagementUseCase userManagementUseCase;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse sampleUser() {
        return new UserResponse(1L, "user@example.com", "johndoe", UserRole.USER,
                AccountStatus.ACTIVE, Instant.now());
    }

    @Test
    void listUsers_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listUsers_withNonAdminRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_withAdminRole_isOk() throws Exception {
        Page<UserResponse> page = new PageImpl<>(List.of(sampleUser()));
        when(userManagementUseCase.listUsers(any())).thenReturn(page);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUser_withAdminRole_isOk() throws Exception {
        when(userManagementUseCase.getUserById(1L)).thenReturn(sampleUser());

        mockMvc.perform(get("/admin/users/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_withAdminRole_isOk() throws Exception {
        when(userManagementUseCase.adminUpdateUser(eq(1L), any())).thenReturn(sampleUser());

        mockMvc.perform(patch("/admin/users/{id}", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AdminUpdateUserRequest("janedoe", null))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_withInvalidUsernamePattern_isBadRequest() throws Exception {
        mockMvc.perform(patch("/admin/users/{id}", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AdminUpdateUserRequest("invalid username!", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void banUser_withAdminRole_isNoContent() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/ban", 1L))
                .andExpect(status().isNoContent());

        verify(userManagementUseCase).banUser(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void banUser_withNonAdminRole_isForbidden() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/ban", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unbanUser_withAdminRole_isNoContent() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/unban", 1L))
                .andExpect(status().isNoContent());

        verify(userManagementUseCase).unbanUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_withAdminRole_isNoContent() throws Exception {
        mockMvc.perform(delete("/admin/users/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userManagementUseCase).deleteUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeRole_withAdminRole_isNoContent() throws Exception {
        mockMvc.perform(patch("/admin/users/{id}/role", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ChangeRoleRequest(UserRole.ADMIN))))
                .andExpect(status().isNoContent());

        verify(userManagementUseCase).changeRole(1L, UserRole.ADMIN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeRole_withNullRole_isBadRequest() throws Exception {
        mockMvc.perform(patch("/admin/users/{id}/role", 1L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
