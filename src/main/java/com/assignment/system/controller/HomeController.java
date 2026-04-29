package com.assignment.system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "<html>" +
                "<head><title>AssignFlow Backend</title></head>" +
                "<body style='font-family: Arial, sans-serif; text-align: center; padding-top: 50px; background-color: #f4f7f6; color: #333;'>"
                +
                "<h1>Welcome to the Backend Workflow of AssignFlow!</h1>" +
                "<p>The API is running successfully.</p>" +
                "</body>" +
                "</html>";
    }
}
