package com.assignment.system.controller;

import com.assignment.system.model.Assignment;
import com.assignment.system.model.Course;
import com.assignment.system.model.Submission;
import com.assignment.system.model.User;
import com.assignment.system.repository.AssignmentRepository;
import com.assignment.system.repository.CourseRepository;
import com.assignment.system.repository.SubmissionRepository;
import com.assignment.system.repository.UserRepository;
import com.assignment.system.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SubmissionRepository submissionRepository;

    private final String UPLOAD_DIR = "uploads/";

    private User getCurrentStudent(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getMyCourses(Authentication auth) {
        User student = getCurrentStudent(auth);
        return ResponseEntity.ok(courseRepository.findByStudentsContaining(student));
    }

    @GetMapping("/courses/{courseId}/assignments")
    public ResponseEntity<?> getAssignments(@PathVariable Long courseId, Authentication auth) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        User student = getCurrentStudent(auth);
        if (!course.getStudents().contains(student)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not enrolled in this course");
        }
        return ResponseEntity.ok(assignmentRepository.findByCourse(course));
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public ResponseEntity<?> submitAssignment(
            @PathVariable Long assignmentId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        User student = getCurrentStudent(auth);

        // Check enrollment
        if (!assignment.getCourse().getStudents().contains(student)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not enrolled in this course");
        }

        Submission submission = submissionRepository.findByStudentAndAssignment(student, assignment)
                .orElse(new Submission());

        submission.setStudent(student);
        submission.setAssignment(assignment);
        submission.setSubmittedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String filename = System.currentTimeMillis() + "_student_submission_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(filename);
                Files.copy(file.getInputStream(), filePath);
                submission.setFileUrl("/" + UPLOAD_DIR + filename);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload file");
            }
        } else {
            return ResponseEntity.badRequest().body("File is missing");
        }

        submissionRepository.save(submission);
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/submissions")
    public ResponseEntity<?> getMySubmissions(Authentication auth) {
        User student = getCurrentStudent(auth);
        return ResponseEntity.ok(submissionRepository.findByStudent(student));
    }
}
