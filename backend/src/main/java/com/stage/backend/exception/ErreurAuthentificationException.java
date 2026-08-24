package com.stage.backend.exception;

import lombok.Getter;
import org.springframework.security.authentication.BadCredentialsException;

@Getter
public class ErreurAuthentificationException extends BadCredentialsException {

    private final String codeErreur;

    public ErreurAuthentificationException(String codeErreur, String message) {
        super(message);
        this.codeErreur = codeErreur;
    }
}
