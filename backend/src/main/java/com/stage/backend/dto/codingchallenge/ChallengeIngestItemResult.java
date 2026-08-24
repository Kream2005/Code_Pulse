package com.stage.backend.dto.codingchallenge;

import com.stage.backend.enums.IngestEntityCase;
import com.stage.backend.enums.IngestItemStatus;

import java.util.List;

public record ChallengeIngestItemResult(
        int index,
        IngestItemStatus status,
        IngestEntityCase entityCase,
        Long testExternalId,
        String testTitre,
        Long userExternalId,
        String userEmail,
        String userName,
        Long challengeId,
        Long userId,
        boolean challengeAlreadyExisted,
        boolean userAlreadyExisted,
        boolean notificationCreated,
        boolean notificationAlreadyExisted,
        List<String> userFieldsUpdated,
        List<String> messages,
        List<String> errors,
        String errorCode
) {
    public static ChallengeIngestItemResult failed(
            int index,
            CodingChallengeEventRef ref,
            String errorCode,
            String errorMessage
    ) {
        return new ChallengeIngestItemResult(
                index,
                IngestItemStatus.FAILED,
                null,
                ref != null ? ref.testExternalId() : null,
                ref != null ? ref.testTitre() : null,
                ref != null ? ref.userExternalId() : null,
                ref != null ? ref.userEmail() : null,
                ref != null ? ref.userName() : null,
                null,
                null,
                false,
                false,
                false,
                false,
                List.of(),
                List.of(),
                List.of(errorMessage),
                errorCode
        );
    }

    public record CodingChallengeEventRef(
            Long testExternalId,
            String testTitre,
            Long userExternalId,
            String userEmail,
            String userName
    ) {}
}
