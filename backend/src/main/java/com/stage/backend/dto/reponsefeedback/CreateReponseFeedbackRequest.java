package com.stage.backend.dto.reponsefeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReponseFeedbackRequest(
        @NotBlank @Size(max = 5000) String valeur,
        @NotNull Long questionFeedbackId
) {}
