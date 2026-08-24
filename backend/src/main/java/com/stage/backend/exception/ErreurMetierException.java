package com.stage.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ErreurMetierException extends RuntimeException {

    private final HttpStatus statut;
    private final String codeErreur;

    public ErreurMetierException(HttpStatus statut, String codeErreur, String message) {
        super(message);
        this.statut = statut;
        this.codeErreur = codeErreur;
    }
}
