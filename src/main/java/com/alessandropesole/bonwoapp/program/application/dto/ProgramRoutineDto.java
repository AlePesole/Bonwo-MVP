package com.alessandropesole.bonwoapp.program.application.dto;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.routine.application.dto.ExerciseSlotDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * One of a TrainingProgram's own Routines. id present + matching an existing Routine owned by this
 * program updates it; id absent creates a brand-new Routine (Routine.trainingProgramId set to this
 * program's id). On update, any existing program Routine whose id isn't present in the submitted list
 * is deleted — same full-list-replace semantics as Routine.slots.
 */
public record ProgramRoutineDto(
        Long id,
        @NotBlank @Size(max = 200) String title,
        String description,
        Level level,
        String thumbnailUploadToken,
        Long thumbnailId,   // used to duplicate an existing owned image's reference instead of re-uploading; ignored if thumbnailUploadToken is also present
        boolean removeThumbnail,
        @Min(1) int position,
        @NotEmpty @Valid List<ExerciseSlotDto> slots,
        Duration restBetweenExercises,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds
) {}
