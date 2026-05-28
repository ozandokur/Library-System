// src/main/java/com/example/LibrarySystem/jwt/SecurityConfig.java
package com.example.LibrarySystem.jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint entryPoint;

    public SecurityConfig(JwtUtil jwtUtil,
                          UserDetailsService userDetailsService,
                          JwtAuthenticationEntryPoint entryPoint) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.entryPoint = entryPoint;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

        http
                .cors(withDefaults())
                // stateless API
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint))

                // on authentication failures use our JSON entry point
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))

                // authorization rules
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/reviews/**").permitAll()
                        .requestMatchers("/api/recommendations/**").permitAll()
                        .requestMatchers("/api/ml-recommendations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/*/cover").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/books/*/cover").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/books/*/cover").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/books/*/cover").permitAll()
                        .requestMatchers(HttpMethod.GET,    "/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/books/**").hasRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT,    "/api/books/**").hasAnyRole("LIBRARIAN", "MEMBER")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.POST,   "/api/members/**").hasRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT,    "/api/members/**").hasAnyRole("LIBRARIAN", "MEMBER")
                        .requestMatchers(HttpMethod.DELETE, "/api/members/**").hasRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.GET,    "/api/members/**").hasAnyRole("LIBRARIAN", "MEMBER")
                        .requestMatchers(HttpMethod.GET,    "/api/users/**").hasAnyRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.POST,    "/api/users/**").hasAnyRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT,    "/api/users/**").hasAnyRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE,    "/api/users/**").hasAnyRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.GET,    "/api/loans/**").hasAnyRole("LIBRARIAN", "MEMBER")
                        .requestMatchers(HttpMethod.POST,    "/api/loans/**").permitAll()
                        .requestMatchers(HttpMethod.PUT,    "/api/loans/**").hasAnyRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE,    "/api/loans/**").hasAnyRole("LIBRARIAN")
                )

                // add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}