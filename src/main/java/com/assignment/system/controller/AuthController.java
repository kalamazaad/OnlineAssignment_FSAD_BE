package com.assignment.system.controller;

import com.assignment.system.model.Role;
import com.assignment.system.model.User;
import com.assignment.system.payload.request.LoginRequest;
import com.assignment.system.payload.request.SignupRequest;
import com.assignment.system.payload.response.JwtResponse;
import com.assignment.system.payload.response.MessageResponse;
import com.assignment.system.repository.UserRepository;
import com.assignment.system.security.JwtUtils;
import com.assignment.system.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Random;
import java.util.stream.Collectors;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

import com.assignment.system.model.PasswordResetToken;
import com.assignment.system.repository.PasswordResetTokenRepository;
import com.assignment.system.payload.request.ForgotPasswordRequest;
import com.assignment.system.payload.request.ResetPasswordRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    // A simple in-memory cache for captcha tokens mapped to expected answers.
    // In production, consider using Redis or an expiring cache like Guava/Caffeine.
    private final Map<String, String> captchaStore = new HashMap<>();

    @GetMapping("/captcha")
    public ResponseEntity<?> generateCaptcha() {
        try {
            // 1. Generate random text
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder captchaText = new StringBuilder();
            Random random = new Random();
            for (int i = 0; i < 6; i++) {
                captchaText.append(chars.charAt(random.nextInt(chars.length())));
            }
            String answer = captchaText.toString();

            // 2. Create the image
            int width = 160;
            int height = 50;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // Set background
            g2d.setColor(new Color(241, 245, 249)); // Slate 100
            g2d.fillRect(0, 0, width, height);

            // Add some noise/lines
            g2d.setColor(new Color(203, 213, 225)); // Slate 300
            for (int i = 0; i < 8; i++) {
                g2d.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width),
                        random.nextInt(height));
            }

            // Draw the text
            g2d.setFont(new Font("Monospaced", Font.BOLD | Font.ITALIC, 32));
            g2d.setColor(new Color(30, 41, 59)); // Slate 800
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw characters with slight random offsets
            for (int i = 0; i < answer.length(); i++) {
                int xOffset = 20 + (i * 20) + random.nextInt(10) - 5;
                int yOffset = 35 + random.nextInt(10) - 5;
                g2d.drawString(String.valueOf(answer.charAt(i)), xOffset, yOffset);
            }
            g2d.dispose();

            // 3. Convert image to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

            // 4. Store and Return
            String token = UUID.randomUUID().toString();
            captchaStore.put(token, answer);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("image", base64Image); // Send the image data instead of text question
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Failed to generate captcha"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // --- Captcha Validation ---
        String expectedAnswer = captchaStore.get(loginRequest.getCaptchaToken());

        if (expectedAnswer == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Captcha token is invalid or expired."));
        }

        if (!expectedAnswer.equals(loginRequest.getCaptcha().trim())) {
            // Optional: Remove token so they must get a new one upon failure
            captchaStore.remove(loginRequest.getCaptchaToken());
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Incorrect Captcha answer. Please try again."));
        }

        // Remove verified captcha from map to prevent reuse attacks
        captchaStore.remove(loginRequest.getCaptchaToken());
        // --------------------------

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getName(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setName(signUpRequest.getName());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        String strRole = signUpRequest.getRole();
        if (strRole == null) {
            user.setRole(Role.ROLE_STUDENT);
        } else {
            if ("ROLE_TEACHER".equals(strRole)) {
                user.setRole(Role.ROLE_TEACHER);
            } else {
                user.setRole(Role.ROLE_STUDENT);
            }
        }

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // User login is their email (username) in this system
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            // We return generic OK message so attackers can't fish for valid emails
            return ResponseEntity.ok(
                    new MessageResponse("If an account with that email exists, a password reset link has been sent."));
        }

        // Generate custom token
        String tokenString = UUID.randomUUID().toString();

        // Delete any old tokens for this user first
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        // Save new token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(tokenString);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // 1 hour expiry
        tokenRepository.save(resetToken);

        // Send Email
        String url = "http://localhost:5173/reset-password?token=" + tokenString;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("AssignFlow - Password Reset Request");
        message.setText(
                "To reset your password, click the link below:\n\n" + url + "\n\nThis link will expire in 1 hour.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Could not send email. Please ensure SMTP configuration is correct."));
        }

        return ResponseEntity
                .ok(new MessageResponse("If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElse(null);

        if (resetToken == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid reset token."));
        }

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Token has expired. Please request a new password reset."));
        }

        User user = resetToken.getUser();
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Token consumed, delete it
        tokenRepository.delete(resetToken);

        return ResponseEntity.ok(new MessageResponse("Password has been successfully reset. You can now login."));
    }
}
