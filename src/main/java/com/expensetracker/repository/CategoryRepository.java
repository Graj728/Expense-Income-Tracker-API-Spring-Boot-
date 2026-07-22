package com.expensetracker.repository;

import com.expensetracker.entity.Category;
import com.expensetracker.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByTypeAndUserIsNullOrTypeAndUserId(
            TransactionType type1, TransactionType type2, Long userId);

    default List<Category> findVisibleToUser(TransactionType type, Long userId) {
        return findByTypeAndUserIsNullOrTypeAndUserId(type, type, userId);
    }
}
