package com.expensetracker.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String name,
        String email
) {}
