package com.campus.portal.controller;

import com.campus.portal.dto.UserDTO;
import com.campus.portal.entity.User;
import com.campus.portal.repository.UserRepository;
import com.campus.portal.repository.ItemRepository;
import com.campus.portal.service.AdminService;
import com.campus.portal.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final JwtUtil jwtUtil;

    public AdminController(AdminService adminService,
                           UserRepository userRepository,
                           ItemRepository itemRepository,
                           JwtUtil jwtUtil) {
        this.adminService = adminService;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.jwtUtil = jwtUtil;
    }

    // =========================================
    // 🔐 CHECK ADMIN JWT
    // =========================================
    private boolean isAdminLoggedIn(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("admin_jwt_token".equals(cookie.getName())) {
                    try {
                        String email = jwtUtil.extractEmail(cookie.getValue());
                        return email != null;
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    // =========================================
    // 🔐 ADMIN LOGIN
    // =========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest request,
                                   HttpServletResponse response) {

        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILURE",
                    "message", "Invalid request"
            ));
        }

        Object admin = adminService.login(
                request.getEmail().trim(),
                request.getPassword().trim()
        );

        if ("SUCCESS".equals(admin)) {
            String token = jwtUtil.generateToken(request.getEmail().trim());
            org.springframework.http.ResponseCookie resCookie = org.springframework.http.ResponseCookie.from("admin_jwt_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .build();
            response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, resCookie.toString());

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "admin", Map.of("email", request.getEmail().trim(), "name", "Admin")
            ));
        }

        return ResponseEntity.status(401).body(Map.of(
                "status", "FAILURE",
                "message", "Invalid credentials"
            ));
    }

    // =========================================
    // 🔐 ADMIN ME
    // =========================================
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        if (!isAdminLoggedIn(request)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String email = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("admin_jwt_token".equals(cookie.getName())) {
                    email = jwtUtil.extractEmail(cookie.getValue());
                    break;
                }
            }
        }
        
        return ResponseEntity.ok(Map.of("email", email != null ? email : "admin@campus.com", "name", "Admin"));
    }

    // =========================================
    // 🔐 ADMIN LOGOUT
    // =========================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        org.springframework.http.ResponseCookie resCookie = org.springframework.http.ResponseCookie.from("admin_jwt_token", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .sameSite("None")
            .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, resCookie.toString());
        return ResponseEntity.ok("Logout successful");
    }

    // =========================================
    // 👥 GET ALL USERS
    // =========================================
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {
        if (!isAdminLoggedIn(request)) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        List<UserDTO> users = userRepository.findAll()
                .stream()
                .map(user -> {
                    UserDTO dto = new UserDTO(user);
                    int count = itemRepository.countByUserId(user.getId());
                    dto.setPostCount(count);
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(users);
    }

    // =========================================
    // 🔍 SEARCH USERS
    // =========================================
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam String name,
                                         HttpServletRequest request) {
        if (!isAdminLoggedIn(request)) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        List<UserDTO> users = userRepository
                .findByFullNameContainingIgnoreCase(name)
                .stream()
                .map(user -> {
                    UserDTO dto = new UserDTO(user);
                    int count = itemRepository.countByUserId(user.getId());
                    dto.setPostCount(count);
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(users);
    }

    // =========================================
    // ❌ DELETE USER
    // =========================================
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        HttpServletRequest request) {
        if (!isAdminLoggedIn(request)) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(404).body("User not found");
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // =========================================
    // 🔄 TOGGLE ACTIVE / INACTIVE
    // =========================================
    @PutMapping("/users/toggle/{id}")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id,
                                              HttpServletRequest request) {
        if (!isAdminLoggedIn(request)) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(!user.isEmailVerified());
        User updatedUser = userRepository.save(user);

        return ResponseEntity.ok(new UserDTO(updatedUser));
    }

    // =========================================
    // 📦 LOGIN DTO
    // =========================================
    public static class AdminLoginRequest {
        private String email;
        private String password;

        public AdminLoginRequest() {}

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
