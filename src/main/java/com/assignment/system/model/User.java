package com.assignment.system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;
import java.util.HashSet;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToMany(mappedBy = "students")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Course> enrolledCourses;

    @OneToMany(mappedBy = "teacher")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Course> taughtCourses;
}
