package com.reglog.service;

import com.reglog.dto.ApiResponse;
import com.reglog.dto.AuthResponse;
import com.reglog.dto.LoginRequest;
import com.reglog.dto.UserResponse;
import com.reglog.entity.JwtToken;
import com.reglog.entity.User;
import com.reglog.exception.InvalidCredentialsException;
import com.reglog.exception.ResourceNotFoundException;
import com.reglog.repository.JwtTokenRepository;
import com.reglog.repository.UserRepository;
import com.reglog.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of AuthService.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final JwtTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    public AuthServiceImpl(UserRepository userRepository,
                           JwtTokenRepository jwtTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        // 1. find the user by username
        User user = userRepository.findByName(request.getUsername())
                .orElse(null);

        // 2. verify the password using BCrypt (never plain-string equality)
        // The two cases (user missing OR wrong password) return the same message
        // so that attackers cannot enumerate which accounts exist.
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // 3. generate the JWT containing userId + username + iat + exp
        String token = jwtUtil.generateToken(user.getId(), user.getName());

        // 4. save token metadata so it can be revoked later
        LocalDateTime now = LocalDateTime.now();
        JwtToken jwtToken = new JwtToken(user, token, now, now.plusNanos(expirationMillis * 1_000_000L));
        jwtTokenRepository.save(jwtToken);

        // 5. add the JWT to an HttpOnly cookie (for development on localhost)
        addJwtCookie(response, token, (int) (expirationMillis / 1000));

        log.info("User '{}' logged in", user.getName());
        return new AuthResponse(true, "Login successful");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone());
    }

    @Override
    @Transactional
    public ApiResponse logout(String token, HttpServletResponse response) {
        if (token != null && !token.isBlank()) {
            // revoke the stored token so it can no longer be used
            jwtTokenRepository.deleteByToken(token);
            log.info("Revoked JWT record for logout");
        }
        // clear the browser cookie
        clearJwtCookie(response);
        return ApiResponse.ok("Logout successful");
    }

    /**
     * Creates the HttpOnly JWT cookie attached to the login response.
     *
     * - HttpOnly: the React app cannot read the token from JavaScript.
     * - Secure=false: required because we are on plain HTTP localhost.
     *   In production with HTTPS, set Secure to true.
     * - SameSite=Lax: allows the cookie to be sent on same-site requests
     *   (localhost:5173 -> localhost:8080 counts as same-site).
     */
    private void addJwtCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0); // delete immediately
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}