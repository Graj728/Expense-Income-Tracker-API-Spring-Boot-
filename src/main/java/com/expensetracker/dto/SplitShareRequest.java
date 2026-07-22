package com.expensetracker.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One member's share for PERCENTAGE or EXACT split types.
 * For PERCENTAGE, `value` is a percentage (0-100).
 * For EXACT, `value` is the exact amount owed.
 */
public record SplitShareRequest(
        @NotNull Long userId,
        @NotNull BigDecimal value
) {}
