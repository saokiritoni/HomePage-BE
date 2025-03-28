package org.farmsystem.homepage.domain.apply.repository;

import org.farmsystem.homepage.domain.apply.entity.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = {"choices"})
    List<Question> findAllByOrderByTrackAscPriorityAsc();
}
