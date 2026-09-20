package com.reglog.repository;

import com.reglog.entity.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the jwt_token table.
 */
@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    Optional<JwtToken> findByToken(String token);

    List<JwtToken> findByUserId(Long userId);

    Optional<JwtToken> findByTokenAndExpiresAtAfter(String token, java.time.LocalDateTime now);

    void deleteByToken(String token);
}