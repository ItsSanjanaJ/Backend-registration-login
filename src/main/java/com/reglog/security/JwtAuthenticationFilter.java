package com.reglog.security;

import com.reglog.repository.JwtTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Spring Security filter that runs BEFORE the authentication filter.
 *
 * Flow:
 * 1. Read the JWT from the "jwt" HttpOnly cookie.
 * 2. Validate the token (signature + expiration) with JwtUtil.
 * 3. Confirm the token is still present in the jwt_token table (not revoked).
 * 4. Build a Spring Security Authentication from the token's username.
 * 5. Store it in the SecurityContext so protected endpoints recognise the user.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenRepository jwtTokenRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   CustomUserDetailsService userDetailsService,
                                   JwtTokenRepository jwtTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.jwtTokenRepository = jwtTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = readCookie(request, "jwt");

        // Already authenticated (e.g. by a previous filter in the chain)
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 1. signature + expiration validation
                String username = jwtUtil.getUsername(token);
                Long userId = jwtUtil.getUserId(token);

                // 2. make sure the token was not revoked on logout
                boolean isStored = jwtTokenRepository
                        .findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                        .isPresent();

                if (username != null && isStored) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 3. build the Spring Security Authentication
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    // 4. store it in the SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user '{}' from JWT cookie (id={})", username, userId);
                } else if (!isStored) {
                    log.debug("JWT not found in token table (likely logged out): {}", token.substring(0, 20));
                }
            } catch (Exception ex) {
                // Invalid/expired token -> leave the context unauthenticated.
                log.debug("JWT validation failed: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}