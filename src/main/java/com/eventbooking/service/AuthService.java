package com.eventbooking.service;

import com.eventbooking.dto.request.LoginRequest;
import com.eventbooking.dto.request.RegisterRequest;
import com.eventbooking.dto.response.AuthResponse;
import com.eventbooking.dto.response.UserResponse;
import com.eventbooking.event.EmailEvent;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.ValidationException;
import com.eventbooking.kafka.KafkaProducerService;
import com.eventbooking.model.User;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final AuthenticationManager authenticationManager;
        private final UserDetailsService userDetailsService;
        private final KafkaProducerService kafkaProducerService;

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new ValidationException("Email already registered");
                }

                User user = User.builder()
                                .name(request.getName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .phone(request.getPhone())
                                .role(request.getRole())
                                .build();

                user = userRepository.save(user);

                // Send welcome email via Kafka
                kafkaProducerService.sendEmailEvent(
                                EmailEvent.builder()
                                                .type(EmailEvent.EmailType.WELCOME)
                                                .toEmail(user.getEmail())
                                                .userName(user.getName())
                                                .build());

                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                String token = jwtUtil.generateToken(userDetails);

                return AuthResponse.builder()
                                .token(token)
                                .email(user.getEmail())
                                .name(user.getName())
                                .role(user.getRole().name())
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                String token = jwtUtil.generateToken(userDetails);

                return AuthResponse.builder()
                                .token(token)
                                .email(user.getEmail())
                                .name(user.getName())
                                .role(user.getRole().name())
                                .build();
        }

        public AuthResponse refreshToken(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                String token = jwtUtil.generateToken(userDetails);

                return AuthResponse.builder()
                                .token(token)
                                .email(user.getEmail())
                                .name(user.getName())
                                .role(user.getRole().name())
                                .build();
        }

        public UserResponse getCurrentUser(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                return UserResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .name(user.getName())
                                .phone(user.getPhone())
                                .role(user.getRole().name())
                                .build();
        }
}
