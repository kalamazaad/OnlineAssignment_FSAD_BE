package com.assignment.system.repository;

import com.assignment.system.model.Assignment;
import com.assignment.system.model.Submission;
import com.assignment.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignment(Assignment assignment);

    Optional<Submission> findByStudentAndAssignment(User student, Assignment assignment);

    List<Submission> findByStudent(User student);
}
