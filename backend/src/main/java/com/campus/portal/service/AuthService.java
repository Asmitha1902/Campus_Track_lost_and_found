package com.campus.portal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.net.InetAddress;

import com.campus.portal.repository.UserRepository;
import com.campus.portal.entity.User;
import com.campus.portal.entity.Role;
import com.campus.portal.dto.*;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${mailboxlayer.api.key}")
    private String mailboxLayerApiKey;

    // ================= REGISTER =================
    public ResponseEntity<String> register(RegisterRequest request) {

        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Invalid email format");
        }

        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        String otp = generateOtp();

        if (existingUser != null) {

            if (existingUser.isEmailVerified()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Email already exists. Please login.");
            }

            existingUser.setOtp(otp);
            existingUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(existingUser);

            try {
                sendOtp(existingUser.getEmail(), otp);
            } catch (Exception e) {
                System.out.println("EMAIL FAILED: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to send OTP email. Please check your email address and try again.");
            }

            return ResponseEntity.ok("OTP resent to your email. Please check inbox and spam.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .studentId(request.getStudentId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .otp(otp)
                .emailVerified(false)
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();

        userRepository.save(user);

        try {
            sendOtp(user.getEmail(), otp);
        } catch (Exception e) {
            System.out.println("EMAIL FAILED: " + e.getMessage());
            // Delete the user so they can retry registration
            userRepository.delete(user);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send OTP email. Please check your email address and try again.");
        }

        return ResponseEntity.ok("OTP sent to your email. Please check inbox and spam.");
    }

    // ================= VERIFY OTP =================
    public ResponseEntity<String> verifyOtp(OtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        if (user.isEmailVerified()) {
            return ResponseEntity.ok("Email already verified");
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("OTP expired");
        }

        if (!user.getOtp().equals(request.getOtp())) {
            return ResponseEntity.badRequest().body("Invalid OTP");
        }

        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("Email verified successfully");
    }

    // ================= LOGIN =================
    public ResponseEntity<?> login(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail()).orElse(null);

    if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Collections.singletonMap("message", "User not found"));
    }

    if (!user.isEmailVerified()) {
        return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", "Verify email first"));
    }

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", "Invalid password"));
    }

    // ✅ Use constructor mapping (BEST PRACTICE)
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

        try {
            sendOtp(user.getEmail(), otp);
        } catch (Exception e) {
            System.out.println("EMAIL FAILED: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send OTP email. Please try again.");
        }

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


    // ================= HELPERS =================

    private String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    private void sendOtp(String email, String otp) {
        // Let exception propagate so caller can handle it properly
        emailService.sendOtpEmail(email, otp);
        System.out.println("OTP SENT SUCCESS to: " + email);
    }
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isEmailDomainValid(String email) {
        try {
            return InetAddress.getByName(email.split("@")[1]) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isEmailExist(String email) {
        try {
            String url = "http://apilayer.net/api/check?access_key=" + mailboxLayerApiKey
                    + "&email=" + email + "&smtp=1&format=1";

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> json = restTemplate.getForObject(url, Map.class);

            return Boolean.TRUE.equals(json.get("smtp_check"));

        } catch (Exception e) {
            return false;
        }
    }
}