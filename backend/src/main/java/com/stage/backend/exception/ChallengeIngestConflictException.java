package com.stage.backend.exception;

import lombok.Getter;

@Getter
public class ChallengeIngestConflictException extends RuntimeException {

    private final String errorCode;
    private final Long incomingUserExternalId;
    private final String incomingEmail;
    private final Long existingUserId;
    private final Long existingUserExternalId;
    private final String existingEmail;

    public ChallengeIngestConflictException(
            String errorCode,
            String message,
            Long incomingUserExternalId,
            String incomingEmail,
            Long existingUserId,
            Long existingUserExternalId,
            String existingEmail
    ) {
        super(message);
        this.errorCode = errorCode;
        this.incomingUserExternalId = incomingUserExternalId;
        this.incomingEmail = incomingEmail;
        this.existingUserId = existingUserId;
        this.existingUserExternalId = existingUserExternalId;
        this.existingEmail = existingEmail;
    }
}
