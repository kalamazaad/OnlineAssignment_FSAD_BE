package com.assignment.system.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String name;

    @NotBlank
    private String password;

    private String role; // "ROLE_STUDENT" or "ROLE_TEACHER"
}
