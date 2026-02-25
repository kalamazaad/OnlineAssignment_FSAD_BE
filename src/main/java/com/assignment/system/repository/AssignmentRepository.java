package com.assignment.system.repository;

import com.assignment.system.model.Assignment;
import com.assignment.system.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourse(Course course);
}
