package com.stage.backend.dto.codingchallenge;

import java.util.List;
import java.util.Map;

public record ChallengeIngestBatchResponse(
        int totalReceived,
        int succeeded,
        int failed,
        Map<String, Integer> entityCaseCounts,
        List<ChallengeIngestItemResult> items
) {}
