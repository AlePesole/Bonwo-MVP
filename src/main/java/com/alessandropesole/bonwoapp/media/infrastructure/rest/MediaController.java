package com.alessandropesole.bonwoapp.media.infrastructure.rest;

import com.alessandropesole.bonwoapp.media.application.dto.ImageUploadResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoUploadResponse;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final CurrentUserResolver currentUserResolver;

    @Operation(
            summary = "Upload a video",
            description = "Uploads a video to the media provider and returns an uploadToken valid for " +
                    "15 minutes. Use the token as mainVideoUploadToken when creating or updating an " +
                    "entity that references it."
    )
    @PostMapping(value = "/videos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.uploadVideo(file, userId));
    }

    @Operation(
            summary = "Upload an image",
            description = "Uploads an image (thumbnail, avatar, icon) to the media provider and returns " +
                    "an uploadToken valid for 15 minutes. Use the token as thumbnailUploadToken when " +
                    "creating or updating an entity that references it."
    )
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.uploadImage(file, userId));
    }
}
