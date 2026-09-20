package com.reglog.service;

import com.reglog.dto.ApiResponse;
import com.reglog.dto.SignupRequest;

/**
 * Business logic for registering new users.
 */
public interface UserService {

    /**
     * Validates the signup request, checks for duplicates, BCrypt-hashes the
     * password and saves a new user. Returns a success/failure lookup.
     */
    ApiResponse signup(SignupRequest request);
}