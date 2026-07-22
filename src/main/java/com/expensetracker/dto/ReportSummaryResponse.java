package com.expensetracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportSummaryResponse(
        String periodFrom,
        String periodTo,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal net,
        List<CategoryBreakdown> incomeByCategory,
        List<CategoryBreakdown> expenseByCategory
) {
    public record CategoryBreakdown(String categoryName, BigDecimal total) {}
}
