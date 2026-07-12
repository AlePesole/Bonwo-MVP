package com.alessandropesole.bonwoapp.media.infrastructure.rest;

import com.alessandropesole.bonwoapp.media.application.dto.ImageUploadResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoUploadResponse;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthEntryPoint;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtAuthenticationFilter;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtService;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.SecurityConfig;
import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthEntryPoint.class})
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;
    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void uploadVideo_withoutAuthentication_isUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "data".getBytes());

        mockMvc.perform(multipart("/media/videos/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void uploadVideo_withAuthenticatedUser_isCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "data".getBytes());
        when(currentUserResolver.resolveId(any())).thenReturn(1L);
        when(mediaService.uploadVideo(any(), eq(1L)))
                .thenReturn(new VideoUploadResponse("token", "thumb", 30, Instant.now()));

        mockMvc.perform(multipart("/media/videos/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uploadToken").value("token"));
    }

    @Test
    void uploadImage_withoutAuthentication_isUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "data".getBytes());

        mockMvc.perform(multipart("/media/images/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void uploadImage_withAuthenticatedUser_isCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "data".getBytes());
        when(currentUserResolver.resolveId(any())).thenReturn(1L);
        when(mediaService.uploadImage(any(), eq(1L)))
                .thenReturn(new ImageUploadResponse("token", "url", Instant.now()));

        mockMvc.perform(multipart("/media/images/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uploadToken").value("token"));
    }
}