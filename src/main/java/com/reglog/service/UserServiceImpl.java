package com.reglog.service;

import com.reglog.dto.ApiResponse;
import com.reglog.dto.SignupRequest;
import com.reglog.entity.User;
import com.reglog.exception.BadRequestException;
import com.reglog.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of UserService.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public ApiResponse signup(SignupRequest request) {
        // 1. duplicate checks
        if (userRepository.existsByName(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // 2. BCrypt hashing - the plain password is never stored
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. save the user
        User user = new User(request.getUsername(), hashedPassword, request.getEmail(), request.getPhone());
        userRepository.save(user);

        return ApiResponse.ok("Registration successful");
    }
}