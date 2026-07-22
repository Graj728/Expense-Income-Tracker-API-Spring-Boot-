package com.expensetracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.enums.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
    List<Transaction> findByGroupIdOrderByDateDesc(Long groupId);

 @Query("""
    SELECT t FROM Transaction t
    WHERE t.user.id = :userId
      AND (CAST(:type AS string) IS NULL OR t.type = :type)
      AND (CAST(:from AS date) IS NULL OR t.date >= :from)
      AND (CAST(:to AS date) IS NULL OR t.date <= :to)
      AND (CAST(:categoryId AS long) IS NULL OR t.category.id = :categoryId)
      AND (:personalOnly = false OR t.group IS NULL)
    ORDER BY t.date DESC
    """)
List<Transaction> search(@Param("userId") Long userId,
                          @Param("type") TransactionType type,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          @Param("categoryId") Long categoryId,
                          @Param("personalOnly") boolean personalOnly);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.user.id = :userId AND t.type = :type
          AND t.date BETWEEN :from AND :to
        """)
    java.math.BigDecimal sumByUserAndTypeAndDateRange(@Param("userId") Long userId,
                                                       @Param("type") TransactionType type,
                                                       @Param("from") LocalDate from,
                                                       @Param("to") LocalDate to);

    @Query("""
        SELECT t.category.name, COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.user.id = :userId AND t.type = :type
          AND t.date BETWEEN :from AND :to
        GROUP BY t.category.name
        ORDER BY SUM(t.amount) DESC
        """)
    List<Object[]> sumGroupedByCategory(@Param("userId") Long userId,
                                         @Param("type") TransactionType type,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}
