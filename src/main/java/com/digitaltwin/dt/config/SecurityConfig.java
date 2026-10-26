package com.digitaltwin.dt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/session").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/timetable/import/template").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/timetable/import").hasRole("ADMIN")
                        .requestMatchers("/api/timetable/import/**").hasRole("ADMIN")
                        .requestMatchers("/api/bookings", "/api/bookings/**", "/api/timetable/cancellations/**").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/rooms", "/api/sections", "/api/teachers",
                                "/api/students", "/api/subjects", "/api/timetable").denyAll()
                        .requestMatchers("/api/**").hasAnyRole("STUDENT", "TEACHER")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    InMemoryUserDetailsManager localUsers() {
        UserDetails admin = User.withUsername("dev")
                .password("{noop}" + env("APP_ADMIN_PASSWORD", env("APP_DEV_PASSWORD", "dev-only-change-me")))
                .roles("ADMIN")
                .build();
        UserDetails student = User.withUsername("student")
                .password("{noop}" + env("APP_STUDENT_PASSWORD", "student-only-change-me"))
                .roles("STUDENT")
                .build();
        UserDetails teacher = User.withUsername("teacher")
                .password("{noop}" + env("APP_TEACHER_PASSWORD", "teacher-only-change-me"))
                .roles("TEACHER")
                .build();
        return new InMemoryUserDetailsManager(admin, student, teacher);
    }

    private String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }
}
