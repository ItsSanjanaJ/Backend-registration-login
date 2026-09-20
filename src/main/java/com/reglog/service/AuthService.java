package com.reglog.service;

import com.reglog.dto.ApiResponse;
import com.reglog.dto.AuthResponse;
import com.reglog.dto.LoginRequest;
import com.reglog.dto.UserResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Business logic for login, current-user lookup and logout.
 */
public interface AuthService {

    /**
     * Verifies the credentials, generates a JWT, persists token metadata and
     * places the JWT in an HttpOnly cookie on the response.
     */
    AuthResponse login(LoginRequest request, HttpServletResponse response);

    /**
     * Returns safe information (no password, no token) for the logged-in user.
     */
    UserResponse getCurrentUser(String username);

    /**
     * Revokes the JWT in the database and clears the cookie.
     */
    ApiResponse logout(String token, HttpServletResponse response);
}