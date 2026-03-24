package com.assignment.system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(nullable = false)
    private String fileUrl; // Path to student's submitted PDF

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private Integer marksReceived;
    private Integer totalMarks;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private boolean isGraded = false;
}
