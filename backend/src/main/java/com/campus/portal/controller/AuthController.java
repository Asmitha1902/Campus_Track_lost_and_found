package com.campus.portal.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.campus.portal.service.AuthService;
import com.campus.portal.dto.*;
import com.campus.portal.security.JwtUtil;
import com.campus.portal.repository.UserRepository;
import com.campus.portal.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // ================= REGISTER =================
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {
        ResponseEntity<?> response = authService.login(request);

        if (!response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        Object body = response.getBody();

        if (body instanceof UserDTO userDTO) {

            // Generate JWT Token
            String token = jwtUtil.generateToken(userDTO.getEmail());

            org.springframework.http.ResponseCookie resCookie = org.springframework.http.ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();
            httpResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, resCookie.toString());

            return ResponseEntity.ok().body(
                    new java.util.HashMap<String, Object>() {
                        {
                            put("message", "Login successful");
                        }
                    });
        }

        return ResponseEntity.status(500).body("Unexpected error");
    }

    // ================= ME =================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("anonymousUser".equals(email)) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        UserDTO userDTO = new UserDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse httpResponse) {
        org.springframework.http.ResponseCookie resCookie = org.springframework.http.ResponseCookie.from("jwt_token", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();
        httpResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, resCookie.toString());
        return ResponseEntity.ok("Logout successful");
    }

    // ================= FORGOT PASSWORD =================
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    // ================= RESET PASSWORD =================
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
