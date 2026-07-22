package com.expensetracker.dto;

import com.expensetracker.entity.enums.TransactionType;

public record CategoryResponse(Long id, String name, TransactionType type, boolean custom) {}
