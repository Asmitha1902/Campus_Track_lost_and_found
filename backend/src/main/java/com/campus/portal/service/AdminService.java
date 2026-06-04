package com.campus.portal.service;

import org.springframework.stereotype.Service;
import com.campus.portal.repository.AdminRepository;
import com.campus.portal.entity.Admin;

import java.util.Optional;

@Service
public class AdminService {

    private final AdminRepository repo;

    public AdminService(AdminRepository repo) {
        this.repo = repo;
    }

    public String login(String email, String password) {

        if ("admin@mlrit.ac.in".equals(email) && "admin123".equals(password)) {
            return "SUCCESS";
        }

        return "Invalid credentials";
    }
}