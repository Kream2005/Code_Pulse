package com.stage.backend.repository;

import com.stage.backend.entity.QuestionFeedback;
import com.stage.backend.enums.TypeQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionFeedbackRepository extends JpaRepository<QuestionFeedback, Long> {
    List<QuestionFeedback> findByLibelle(String libelle);
    List<QuestionFeedback> findByType(TypeQuestion type);
    List<QuestionFeedback> findByObligatoire(Boolean obligatoire);

    List<QuestionFeedback> findBySupprimeFalse();

    Page<QuestionFeedback> findBySupprimeFalse(Pageable pageable);

    long countBySupprimeFalse();
}
