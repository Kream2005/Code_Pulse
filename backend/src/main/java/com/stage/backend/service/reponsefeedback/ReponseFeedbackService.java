package com.stage.backend.service.reponsefeedback;

import com.stage.backend.dto.common.SuppressionResponse;
import com.stage.backend.dto.reponsefeedback.CreateReponseFeedbackRequest;
import com.stage.backend.dto.reponsefeedback.ReponseFeedbackResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ReponseFeedbackService {

    ReponseFeedbackResponse ajouterReponse(CreateReponseFeedbackRequest request);

    SuppressionResponse supprimerReponse(Long reponseId);

    ReponseFeedbackResponse getReponse(Long reponseId);

    List<ReponseFeedbackResponse> getAllReponses();

    Page<ReponseFeedbackResponse> getReponsesPage(int page, int size);

    List<ReponseFeedbackResponse> getReponsesByQuestion(Long questionFeedbackId);

    long countReponses();

    boolean existsById(Long reponseId);
}