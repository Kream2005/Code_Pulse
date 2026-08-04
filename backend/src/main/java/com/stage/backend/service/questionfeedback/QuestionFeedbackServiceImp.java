package com.stage.backend.service.questionfeedback;

import com.stage.backend.dto.questionfeedback.CreateQuestionFeedbackRequest;
import com.stage.backend.dto.questionfeedback.QuestionFeedbackResponse;
import com.stage.backend.entity.QuestionFeedback;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.enums.TypeQuestion;
import com.stage.backend.mapper.QuestionFeedbackMapper;
import com.stage.backend.repository.QuestionFeedbackRepository;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuestionFeedbackServiceImp implements QuestionFeedbackService{

    private final QuestionFeedbackRepository repository;
    private final QuestionFeedbackMapper mapper;
    private final IntegrationLogService integrationLogService;

    @Override
    public QuestionFeedbackResponse ajouterQuestion(CreateQuestionFeedbackRequest request) {
        log.info("Creating question: '{}'", request.libelle());

        QuestionFeedback question = mapper.toEntity(request);
        applyChoices(question, request.type(), request.choix());
        QuestionFeedback savedQuestion = repository.save(question);
        integrationLogService.logEvent(
                TypeLog.FEEDBACK,
                StatutLog.SUCCES,
                "Question created: " + savedQuestion.getLibelle(),
                null
        );

        return mapper.toQuestionFeedbackResponse(savedQuestion);
    }

    @Override
    public QuestionFeedbackResponse modifierQuestion(QuestionFeedbackResponse request, Long questionId) {
        QuestionFeedback question = repository.findById(questionId).orElseThrow(
                () -> new EntityNotFoundException(
                        "Question with id: " + questionId + "was not found"
                )
        );

        question.setLibelle(request.libelle());
        question.setType(request.type());
        question.setObligatoire(request.obligatoire());
        applyChoices(question, request.type(), request.choix());

        QuestionFeedback updated = repository.save(question);
        integrationLogService.logEvent(
                TypeLog.FEEDBACK,
                StatutLog.SUCCES,
                "Question updated: " + updated.getLibelle(),
                null
        );
        return mapper.toQuestionFeedbackResponse(updated);
    }

    @Override
    public boolean supprimerQuestion(Long questionId) {
        return repository.findById(questionId).map(q -> {
            q.setSupprime(true);
            repository.save(q);
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.SUCCES,
                    "Question soft-deleted (answers kept): " + q.getLibelle(),
                    null
            );
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionFeedbackResponse getQuestion(Long questionId) {
        QuestionFeedback question = repository.findById(questionId).orElseThrow(
                () -> new EntityNotFoundException(
                        "Question with id: " + questionId + "was not found"
                )
        );
        return mapper.toQuestionFeedbackResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionFeedbackResponse> getAllQuestions() {
        return repository.findBySupprimeFalse()
                .stream()
                .map(mapper::toQuestionFeedbackResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionFeedbackResponse> getQuestionsPage(int page, int size) {
        return repository.findBySupprimeFalse(PageRequest.of(page, size))
                         .map(mapper::toQuestionFeedbackResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionFeedbackResponse> searchQuestions(String keyword, TypeQuestion type, int page, int size) {
        String normalized = keyword == null ? "" : keyword.trim();
        return repository.search(normalized, type, PageRequest.of(page, size))
                         .map(mapper::toQuestionFeedbackResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionFeedbackResponse> getQuestionsByType(TypeQuestion type) {
        return repository.findByType(type)
                .stream()
                .map(mapper::toQuestionFeedbackResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionFeedbackResponse> getQuestionsByObligatoire(boolean obligatoire) {
        return repository.findByObligatoire(obligatoire)
                .stream()
                .map(mapper::toQuestionFeedbackResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countQuestions() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long questionId) {
        return repository.existsById(questionId);
    }

    private void applyChoices(QuestionFeedback question, TypeQuestion type, List<String> rawChoix) {
        if (type == TypeQuestion.CHOIX) {
            List<String> cleaned = normalizeChoices(rawChoix);
            if (cleaned.size() < 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A CHOIX question requires at least 2 options"
                );
            }
            question.setChoix(cleaned);
            return;
        }
        question.setChoix(new ArrayList<>());
    }

    private List<String> normalizeChoices(List<String> rawChoix) {
        if (rawChoix == null) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String option : rawChoix) {
            if (option == null) {
                continue;
            }
            String trimmed = option.trim();
            if (!trimmed.isEmpty()) {
                unique.add(trimmed);
            }
        }
        return new ArrayList<>(unique);
    }
}
