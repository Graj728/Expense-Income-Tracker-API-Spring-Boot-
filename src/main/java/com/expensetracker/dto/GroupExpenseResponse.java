package com.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GroupExpenseResponse(
        Long transactionId,
        BigDecimal amount,
        LocalDate date,
        String description,
        Long paidByUserId,
        List<SplitLine> splits
) {
    public record SplitLine(Long userId, String userName, BigDecimal shareAmount, boolean paid) {}
}
