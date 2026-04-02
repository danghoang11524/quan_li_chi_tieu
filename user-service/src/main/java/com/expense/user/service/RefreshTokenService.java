package com.expense.user.service;

import com.expense.common.util.JwtUtil;
import com.expense.user.entity.RefreshToken;
import com.expense.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public RefreshToken createRefreshToken(Long userId, String username, String deviceInfo) {
        // Generate unique refresh token using JWT
        String tokenString = jwtUtil.generateRefreshToken(username, userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .deviceInfo(deviceInfo)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }
        if (token.getRevoked()) {
            throw new RuntimeException("Refresh token đã bị thu hồi. Vui lòng đăng nhập lại.");
        }
        return token;
    }

    @Transactional
    public void revokeUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    @Transactional
    public void deleteUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    // Clean up expired tokens every day at 3 AM
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens");
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Completed cleanup of expired refresh tokens");
    }
}
