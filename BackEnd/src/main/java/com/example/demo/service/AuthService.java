package com.example.demo.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.Security.JwtService;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignUpRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.AuthRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtservice;

    public ResponseEntity<Object> saveUserData(SignUpRequest userData) {
        Optional<User> existing_user = authRepository.findByEmail(userData.getEmail());
        if (existing_user.isPresent()) {
            return new ResponseEntity<>("User already exists", HttpStatus.CONFLICT);
        }

        User user = new User();

        user.setEmail(userData.getEmail());
        user.setFirstName(userData.getFirstName());
        user.setLastName(userData.getLastName());
        user.setPassword(passwordEncoder.encode(userData.getPassword()));
        user.setRole(Role.ROLE_USER);

        authRepository.save(user);
        return new ResponseEntity<>("User created successfully", HttpStatus.CREATED);
    }

    public AuthResponse loginRequest(LoginRequest loginData) {
        User user_data = authRepository.findByEmail(loginData.getEmail())
                .orElseThrow(() -> new RuntimeException("User Not found"));

        if (!passwordEncoder.matches(loginData.getPassword(), user_data.getPassword())) {
            throw new RuntimeException("Wrong password");
        }
        String token = jwtservice.generate_token(user_data);
        return new AuthResponse(token);
    }

}
