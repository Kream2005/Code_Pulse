package com.stage.backend.service.reponsefeedback;

import com.stage.backend.dto.common.SuppressionResponse;
import com.stage.backend.dto.reponsefeedback.CreateReponseFeedbackRequest;
import com.stage.backend.dto.reponsefeedback.ReponseFeedbackResponse;
import com.stage.backend.entity.ReponseFeedback;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.mapper.ReponseFeedbackMapper;
import com.stage.backend.repository.ReponseFeedbackRepository;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReponseFeedbackServiceImp implements ReponseFeedbackService {

    private final ReponseFeedbackRepository repository;
    private final ReponseFeedbackMapper mapper;
    private final IntegrationLogService integrationLogService;

    @Override
    public ReponseFeedbackResponse ajouterReponse(CreateReponseFeedbackRequest request) {
        log.info("Creating response");

        ReponseFeedback reponse = mapper.toEntity(request);
        ReponseFeedback savedReponse = repository.save(reponse);
        integrationLogService.logEvent(
                TypeLog.FEEDBACK,
                StatutLog.SUCCES,
                "Response created for question " + request.questionFeedbackId(),
                null
        );

        return mapper.toReponseFeedbackResponse(savedReponse);
    }

    @Override
    public SuppressionResponse supprimerReponse(Long reponseId) {
        return repository.findById(reponseId).map(r -> {
            r.setSupprime(true);
            repository.save(r);
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.SUCCES,
                    "Réponse supprimée (soft-delete) : id=" + r.getId(),
                    null
            );
            return new SuppressionResponse(
                    true,
                    reponseId,
                    "REPONSE_FEEDBACK",
                    true,
                    0,
                    "Réponse archivée avec succès"
            );
        }).orElseGet(() -> new SuppressionResponse(
                false,
                reponseId,
                "REPONSE_FEEDBACK",
                true,
                0,
                "Réponse introuvable : id=" + reponseId
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ReponseFeedbackResponse getReponse(Long reponseId) {
        ReponseFeedback reponse = repository.findById(reponseId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Response with id: "
                                        + reponseId
                                        + " was not found"
                        )
                );

        return mapper.toReponseFeedbackResponse(reponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReponseFeedbackResponse> getAllReponses() {
        return repository.findAll()
                .stream()
                .map(mapper::toReponseFeedbackResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReponseFeedbackResponse> getReponsesPage(
            int page,
            int size
    ) {
        return repository.findAll(PageRequest.of(page, size))
                .map(mapper::toReponseFeedbackResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReponseFeedbackResponse> getReponsesByQuestion(
            Long questionFeedbackId
    ) {
        return repository.findByQuestionFeedbackId(questionFeedbackId)
                .stream()
                .map(mapper::toReponseFeedbackResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countReponses() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long reponseId) {
        return repository.existsById(reponseId);
    }
}