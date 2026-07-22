package com.expensetracker.dto;

import com.expensetracker.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        LocalDate date,
        String description,
        Long categoryId,
        String categoryName,
        Long groupId,
        String groupName,
        Long paidById
) {}
