package com.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record GroupRequest(
        @NotBlank String name,
        String description,
        List<Long> memberUserIds   // initial members to add besides the creator
) {}
