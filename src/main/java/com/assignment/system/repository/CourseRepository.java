package com.assignment.system.repository;

import com.assignment.system.model.Course;
import com.assignment.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacher(User teacher);

    List<Course> findByStudentsContaining(User student);
}
