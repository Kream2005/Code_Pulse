package com.stage.backend.service.codingchallenge;

import com.stage.backend.dto.codingchallenge.CodingChallengeDto;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

public interface CodingChallengeService {
    void processIncomingChallenge(CodingChallengeEvent event);

    CodingChallengeDto getCodingChallenge(Long challengeId);

    List<CodingChallengeDto> getAllCodingChallenges();

    /** Soft-delete: keeps feedbacks readable for admins. */
    boolean supprimerCodingChallenge(Long challengeId);

    Page<CodingChallengeDto> getCodingChallengesPage(int page, int size);

    Page<CodingChallengeDto> searchCodingChallenges(String keyword, String tag, int page, int size);

    List<String> getDistinctTags();

    List<CodingChallengeDto> rechercherChallengesByTitre(String titre);

    List<CodingChallengeDto> getChallengesByDescription(String description);

    List<CodingChallengeDto> getChallengesByDuree(Integer duree);

    List<CodingChallengeDto> getChallengesByDateCompletion(ZonedDateTime dateCompletion);

    long countCodingChallenges();

    int synchroniserChallenges();

    int ingestBatch(List<CodingChallengeEvent> events);
}
