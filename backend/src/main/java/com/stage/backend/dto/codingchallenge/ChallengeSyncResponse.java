package com.stage.backend.dto.codingchallenge;

import java.util.List;

public record ChallengeSyncResponse(
        String mode,
        boolean externalApiEnabled,
        boolean kafkaEnabled,
        int fetchedFromPublisher,
        int publishedOrProcessed,
        int succeeded,
        int failed,
        String message,
        List<ChallengeIngestItemResult> directIngestResults
) {}
