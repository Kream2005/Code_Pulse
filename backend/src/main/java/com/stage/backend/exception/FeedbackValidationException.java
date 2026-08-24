package com.stage.backend.exception;

import com.stage.backend.dto.common.ErreurValidation;
import lombok.Getter;

import java.util.List;

@Getter
public class FeedbackValidationException extends RuntimeException {

    private final List<ErreurValidation> erreurs;

    public FeedbackValidationException(String message, List<ErreurValidation> erreurs) {
        super(message);
        this.erreurs = erreurs;
    }
}
