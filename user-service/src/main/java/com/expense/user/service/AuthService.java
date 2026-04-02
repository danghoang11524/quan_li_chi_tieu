package com.expense.user.service;

import com.expense.common.exception.BadRequestException;
import com.expense.common.exception.UnauthorizedException;
import com.expense.common.util.JwtUtil;
import com.expense.user.dto.AuthResponse;
import com.expense.user.dto.LoginRequest;
import com.expense.user.dto.RegisterRequest;
import com.expense.user.dto.UserResponse;
import com.expense.user.entity.RefreshToken;
import com.expense.user.entity.User;
import com.expense.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, null);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String deviceInfo) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .monthlyIncome(request.getMonthlyIncome())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), user.getUsername(), deviceInfo);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .email(user.getEmail())
                .userId(user.getId())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), user.getUsername(), deviceInfo);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .email(user.getEmail())
                .userId(user.getId())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        String username = jwtUtil.extractUsername(refreshTokenStr);
        Long userId = refreshToken.getUserId();

        String newAccessToken = jwtUtil.generateToken(username, userId);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshTokenStr)
                .username(username)
                .userId(userId)
                .build();
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, com.expense.user.dto.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Check if email is being changed and if it's already taken by another user
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getCurrency() != null) {
            user.setCurrency(request.getCurrency());
        }

        if (request.getMonthlyIncome() != null) {
            user.setMonthlyIncome(request.getMonthlyIncome());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, com.expense.user.dto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .currency(user.getCurrency())
                .monthlyIncome(user.getMonthlyIncome())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
