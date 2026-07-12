package com.alessandropesole.bonwoapp.catalog.infrastructure.rest;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.ActivityUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.EquipmentUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.TrainingGoalUseCase;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthEntryPoint;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthenticationFilter;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtService;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.SecurityConfig;
import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthEntryPoint.class})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ActivityUseCase activityUseCase;
    @MockitoBean
    private EquipmentUseCase equipmentUseCase;
    @MockitoBean
    private TrainingGoalUseCase trainingGoalUseCase;
    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listActivities_isPubliclyAccessible() throws Exception {
        when(activityUseCase.listAll()).thenReturn(
                List.of(new ActivityResponse(1L, "Running", "Cardio", null)));

        mockMvc.perform(get("/catalog/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Running"));
    }

    @Test
    void createActivity_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(post("/catalog/activities")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ActivityRequest("Running", "Cardio", "token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createActivity_withNonAdminRole_isForbidden() throws Exception {
        mockMvc.perform(post("/catalog/activities")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ActivityRequest("Running", "Cardio", "token"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createActivity_withAdminRoleAndValidBody_isCreated() throws Exception {
        when(currentUserResolver.resolveId(any())).thenReturn(1L);
        when(activityUseCase.create(any(ActivityRequest.class), eq(1L)))
                .thenReturn(new ActivityResponse(1L, "Running", "Cardio", null));

        mockMvc.perform(post("/catalog/activities")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ActivityRequest("Running", "Cardio", "token"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Running"));

        verify(activityUseCase).create(any(ActivityRequest.class), eq(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createActivity_withBlankName_isBadRequest() throws Exception {
        mockMvc.perform(post("/catalog/activities")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ActivityRequest("", "Cardio", "token"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteActivity_withAdminRole_isNoContent() throws Exception {
        mockMvc.perform(delete("/catalog/activities/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(activityUseCase).delete(1L);
    }
}