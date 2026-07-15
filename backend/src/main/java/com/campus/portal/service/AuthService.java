package com.campus.portal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Random;

import com.campus.portal.repository.UserRepository;
import com.campus.portal.entity.User;
import com.campus.portal.entity.Role;
import com.campus.portal.dto.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${mailboxlayer.api.key:}")
    private String mailboxLayerApiKey;

    // ================= REGISTER =================
    public ResponseEntity<String> register(RegisterRequest request) {

        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Invalid email format");
        }

        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already exists. Please login.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .studentId(request.getStudentId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .emailVerified(true)   // ✅ Auto-verified, no OTP needed
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("Account created successfully! You can now login.");
    }

    // ================= LOGIN =================
    public ResponseEntity<?> login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("message", "User not found"));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("message", "Invalid password"));
        }

        UserDTO dto = new UserDTO(user);
        return ResponseEntity.ok(dto);
    }

    // ================= FORGOT PASSWORD =================
    public ResponseEntity<String> forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not registered");
        }

        String otp = generateOtp();

        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // NOTE: Email sending removed. OTP is saved in DB.
        // To enable email, wire in EmailService and call emailService.sendOtpEmail(user.getEmail(), otp)
        System.out.println("FORGOT PASSWORD OTP for " + user.getEmail() + ": " + otp);

        return ResponseEntity.ok("OTP sent to your email. Please check inbox and spam.");
    }

    // ================= RESET PASSWORD =================
    public ResponseEntity<String> resetPassword(ResetPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        if (user.getOtp() == null ||
                user.getOtpExpiry() == null ||
                !user.getOtp().equals(request.getOtp()) ||
                user.getOtpExpiry().isBefore(LocalDateTime.now())) {

            return ResponseEntity.badRequest().body("Invalid or expired OTP");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successful");
    }

    // ================= HELPERS =================
    private String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}