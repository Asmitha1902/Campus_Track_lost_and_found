package com.campus.portal.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    public void sendOtpEmail(String toEmail, String otp) {

        String url = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("from", "Campus Track <onboarding@resend.dev>");
        body.put("to", List.of(toEmail));
        body.put("subject", "Campus Portal - Email Verification OTP");
        body.put("html",
                "<h2>Campus Portal OTP Verification</h2>" +
                        "<p>Hello,</p>" +
                        "<p>Your OTP for email verification is:</p>" +
                        "<h1>" + otp + "</h1>" +
                        "<p>This OTP is valid for 5 minutes.</p>" +
                        "<p>Thank You.</p>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send OTP email: " + response.getBody());
        }

        System.out.println("OTP EMAIL SENT TO: " + toEmail);
    }
}