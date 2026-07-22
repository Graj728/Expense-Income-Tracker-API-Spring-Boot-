package com.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementResponse(
        Long id,
        Long groupId,
        Long paidByUserId,
        String paidByName,
        Long paidToUserId,
        String paidToName,
        BigDecimal amount,
        LocalDateTime settledAt,
        String note
) {}
