package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SettlementRequest(
        @NotNull Long paidByUserId,
        @NotNull Long paidToUserId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String note
) {}
