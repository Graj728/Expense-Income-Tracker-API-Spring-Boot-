package com.expensetracker.dto;

import com.expensetracker.entity.enums.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GroupExpenseRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate date,
        String description,
        @NotNull Long categoryId,
        @NotNull Long paidByUserId,          // who fronted the money
        @NotNull SplitType splitType,
        /**
         * Which group members participate in the split.
         * Required for EQUAL. Ignored (derived from shares) for PERCENTAGE/EXACT.
         */
        List<Long> participantUserIds,
        /** Required for PERCENTAGE and EXACT split types. */
        List<SplitShareRequest> shares
) {}
