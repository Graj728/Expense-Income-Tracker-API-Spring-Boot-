package com.expensetracker.service;

import com.expensetracker.dto.AuthResponse;
import com.expensetracker.dto.LoginRequest;
import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.entity.RefreshToken;
import com.expensetracker.entity.User;
import com.expensetracker.exception.EmailAlreadyExistsException;
import com.expensetracker.repository.RefreshTokenRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtService;
import com.expensetracker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        user = userRepository.save(user);

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        UserPrincipal principal = new UserPrincipal(user);

        if (!jwtService.isRefreshToken(refreshToken) || !jwtService.isTokenValid(refreshToken, principal)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String tokenId = jwtService.extractTokenId(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new BadCredentialsException("Refresh token was not recognized"));

        if (stored.isRevoked()) {
            throw new BadCredentialsException("This session has been signed out. Please log in again");
        }

        // Rotate: revoke the used refresh token and issue a fresh pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokenPair(user);
    }

    /** Revokes a single refresh token (used on logout). Silently no-ops on an already-invalid token. */
    @Transactional
    public void logout(String refreshToken) {
        try {
            String tokenId = jwtService.extractTokenId(refreshToken);
            if (tokenId == null) return;
            refreshTokenRepository.findByTokenId(tokenId).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        } catch (Exception ignored) {
            // Token was already malformed/expired — nothing to revoke.
        }
    }

    private AuthResponse issueTokenPair(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);

        String tokenId = jwtService.newTokenId();
        String refreshToken = jwtService.generateRefreshToken(principal, tokenId);

        RefreshToken record = RefreshToken.builder()
                .user(user)
                .tokenId(tokenId)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtService.getRefreshTokenExpirationMs())))
                .build();
        refreshTokenRepository.save(record);

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getName(), user.getEmail());
    }
}

