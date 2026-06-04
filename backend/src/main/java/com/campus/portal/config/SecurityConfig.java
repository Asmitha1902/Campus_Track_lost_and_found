package com.campus.portal.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import com.campus.portal.security.JwtFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ❌ Disable CSRF
                .csrf(csrf -> csrf.disable())

                // ✅ Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 🔐 Authorization rules
                .authorizeHttpRequests(auth -> auth

                        // ✅ Preflight (CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ AUTH APIs (REGISTER + LOGIN)
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ ADMIN APIs
                        .requestMatchers("/api/admin/**").permitAll()
                        .requestMatchers("/api/reports").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/items/approve/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/items/reject/**").permitAll()

                        // ✅ PUBLIC APIs
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items/**").permitAll()

                        // 🔒 PROTECTED APIs
                        .requestMatchers("/api/items/my-posts").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/items/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/items/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/items/**").authenticated()

                        // बाकी सब protected
                        .anyRequest().authenticated())

                // ❌ Disable default login UI
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())

                // ✅ IMPORTANT: STATELESS SESSION FOR JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Add JWT filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🌍 CORS Configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://*.vercel.app"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // ✅ REQUIRED for cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}