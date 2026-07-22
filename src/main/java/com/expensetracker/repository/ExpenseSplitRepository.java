package com.expensetracker.repository;

import com.expensetracker.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {
    List<ExpenseSplit> findByTransactionId(Long transactionId);

    List<ExpenseSplit> findByTransactionGroupId(Long groupId);

    List<ExpenseSplit> findByUserIdAndTransactionGroupId(Long userId, Long groupId);
}
