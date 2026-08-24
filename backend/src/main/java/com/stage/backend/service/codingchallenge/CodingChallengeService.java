package com.stage.backend.service.codingchallenge;

import com.stage.backend.dto.codingchallenge.ChallengeDeleteResponse;
import com.stage.backend.dto.codingchallenge.ChallengeIngestBatchResponse;
import com.stage.backend.dto.codingchallenge.ChallengeIngestItemResult;
import com.stage.backend.dto.codingchallenge.ChallengeSyncResponse;
import com.stage.backend.dto.codingchallenge.CodingChallengeDto;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

public interface CodingChallengeService {
    void processIncomingChallenge(CodingChallengeEvent event);

    ChallengeIngestItemResult processIncomingChallengeDetailed(CodingChallengeEvent event, int index);

    CodingChallengeDto getCodingChallenge(Long challengeId);

    List<CodingChallengeDto> getAllCodingChallenges();

    ChallengeDeleteResponse supprimerCodingChallenge(Long challengeId);

    Page<CodingChallengeDto> getCodingChallengesPage(int page, int size);

    Page<CodingChallengeDto> searchCodingChallenges(String keyword, String tag, int page, int size);

    List<String> getDistinctTags();

    List<CodingChallengeDto> rechercherChallengesByTitre(String titre);

    List<CodingChallengeDto> getChallengesByDescription(String description);

    List<CodingChallengeDto> getChallengesByDuree(Integer duree);

    List<CodingChallengeDto> getChallengesByDateCompletion(ZonedDateTime dateCompletion);

    long countCodingChallenges();

    ChallengeSyncResponse synchroniserChallenges();

    ChallengeIngestBatchResponse ingestBatch(List<CodingChallengeEvent> events);
}
