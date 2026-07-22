package com.expensetracker.service;

import com.expensetracker.dto.ReportSummaryResponse;
import com.expensetracker.entity.enums.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public ReportSummaryResponse summary(Long userId, LocalDate from, LocalDate to) {
        BigDecimal totalIncome = transactionRepository.sumByUserAndTypeAndDateRange(userId, TransactionType.INCOME, from, to);
        BigDecimal totalExpense = transactionRepository.sumByUserAndTypeAndDateRange(userId, TransactionType.EXPENSE, from, to);

        List<ReportSummaryResponse.CategoryBreakdown> incomeByCategory =
                transactionRepository.sumGroupedByCategory(userId, TransactionType.INCOME, from, to).stream()
                        .map(row -> new ReportSummaryResponse.CategoryBreakdown((String) row[0], (BigDecimal) row[1]))
                        .toList();

        List<ReportSummaryResponse.CategoryBreakdown> expenseByCategory =
                transactionRepository.sumGroupedByCategory(userId, TransactionType.EXPENSE, from, to).stream()
                        .map(row -> new ReportSummaryResponse.CategoryBreakdown((String) row[0], (BigDecimal) row[1]))
                        .toList();

        return new ReportSummaryResponse(
                from.toString(), to.toString(),
                totalIncome, totalExpense, totalIncome.subtract(totalExpense),
                incomeByCategory, expenseByCategory
        );
    }
}
