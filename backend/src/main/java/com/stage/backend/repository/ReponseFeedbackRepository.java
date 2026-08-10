package com.stage.backend.repository;

import com.stage.backend.entity.ReponseFeedback;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReponseFeedbackRepository extends JpaRepository<ReponseFeedback, Long> {

    List<ReponseFeedback> findByQuestionFeedbackId(Long questionFeedbackId);

    @EntityGraph(attributePaths = {"questionFeedback"})
    List<ReponseFeedback> findByFeedbackId(Long feedbackId);
}
