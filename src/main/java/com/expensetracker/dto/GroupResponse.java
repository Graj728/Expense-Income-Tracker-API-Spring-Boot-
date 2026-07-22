package com.expensetracker.dto;

import java.util.List;

public record GroupResponse(
        Long id,
        String name,
        String description,
        Long createdById,
        List<MemberResponse> members
) {
    public record MemberResponse(Long userId, String name, String email, String role) {}
}
