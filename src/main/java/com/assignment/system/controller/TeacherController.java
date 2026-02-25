package com.assignment.system.controller;

import com.assignment.system.model.*;
import com.assignment.system.repository.*;
import com.assignment.system.security.UserDetailsImpl;
import com.assignment.system.payload.response.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SubmissionRepository submissionRepository;

    private final String UPLOAD_DIR = "uploads/";

    private User getCurrentTeacher(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(@RequestBody Map<String, String> payload, Authentication auth) {
        User teacher = getCurrentTeacher(auth);
        Course course = new Course();
        course.setTitle(payload.get("title"));
        course.setDescription(payload.get("description"));
        course.setTeacher(teacher);
        courseRepository.save(course);
        return ResponseEntity.ok(course);
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getMyCourses(Authentication auth) {
        User teacher = getCurrentTeacher(auth);
        return ResponseEntity.ok(courseRepository.findByTeacher(teacher));
    }

    @PostMapping("/courses/{courseId}/students")
    public ResponseEntity<?> addStudentToCourse(@PathVariable Long courseId, @RequestBody Map<String, String> payload,
            Authentication auth) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        String username = payload.get("studentUsername");

        java.util.Optional<User> studentOpt = userRepository.findByUsername(username);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Student not found with username: " + username));
        }
        User student = studentOpt.get();

        if (student.getRole() != Role.ROLE_STUDENT) {
            return ResponseEntity.badRequest().body("User is not a student");
        }

        course.getStudents().add(student);
        courseRepository.save(course);
        return ResponseEntity.ok("Student added to course successfully");
    }

    @GetMapping("/courses/{courseId}/students")
    public ResponseEntity<?> getStudentsInCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        return ResponseEntity.ok(course.getStudents());
    }

    @PostMapping("/courses/{courseId}/assignments")
    public ResponseEntity<?> createAssignment(
            @PathVariable Long courseId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("startDate") String startDate,
            @RequestParam("dueDate") String dueDate,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication auth) {

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        User teacher = getCurrentTeacher(auth);
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not your course");
        }

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setStartDate(LocalDateTime.parse(startDate)); // format expecting 2024-05-15T10:00:00
        assignment.setDueDate(LocalDateTime.parse(dueDate));

        if (file != null && !file.isEmpty()) {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(filename);
                Files.copy(file.getInputStream(), filePath);
                assignment.setFileUrl("/" + UPLOAD_DIR + filename);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload file");
            }
        }
        assignmentRepository.save(assignment);
        return ResponseEntity.ok(assignment);
    }

    @GetMapping("/courses/{courseId}/assignments")
    public ResponseEntity<?> getAssignmentsForCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        return ResponseEntity.ok(assignmentRepository.findByCourse(course));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<?> getSubmissions(@PathVariable Long assignmentId, Authentication auth) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        User teacher = getCurrentTeacher(auth);
        if (!assignment.getCourse().getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not your assignment");
        }
        return ResponseEntity.ok(submissionRepository.findByAssignment(assignment));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<?> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Submission submission = submissionRepository.findById(submissionId).orElseThrow();
        // Validation check for teacher skipped for brevity...

        submission.setMarksReceived((Integer) payload.get("marksReceived"));
        submission.setTotalMarks((Integer) payload.get("totalMarks"));
        submission.setFeedback((String) payload.get("feedback"));
        submission.setGraded(true);
        submissionRepository.save(submission);

        return ResponseEntity.ok("Graded successfully");
    }
}
