package com.expensetracker.service;

import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.dto.TransactionResponse;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.User;
import com.expensetracker.entity.enums.TransactionType;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = categoryService.getById(request.categoryId());

        if (category.getType() != request.type()) {
            throw new BadRequestException("Category type does not match transaction type");
        }

        // Personal transactions created through this endpoint never belong to a group.
        // Income is always personal by design; personal expenses also have group = null.
        Transaction transaction = Transaction.builder()
                .type(request.type())
                .amount(request.amount())
                .date(request.date())
                .description(request.description())
                .category(category)
                .user(user)
                .group(null)
                .paidBy(request.type() == TransactionType.EXPENSE ? user : null)
                .build();

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> search(Long userId, TransactionType type, LocalDate from, LocalDate to,
                                             Long categoryId, boolean personalOnly) {
        return transactionRepository.search(userId, type, from, to, categoryId, personalOnly).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getOwned(Long userId, Long transactionId) {
        Transaction t = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return toResponse(t);
    }

    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (transaction.getGroup() != null) {
            throw new BadRequestException("Group expenses must be edited through the group expense endpoint");
        }

        Category category = categoryService.getById(request.categoryId());
        if (category.getType() != request.type()) {
            throw new BadRequestException("Category type does not match transaction type");
        }

        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDate(request.date());
        transaction.setDescription(request.description());
        transaction.setCategory(category);

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (transaction.getGroup() != null) {
            throw new BadRequestException("Group expenses must be deleted through the group expense endpoint");
        }

        transactionRepository.delete(transaction);
    }

    TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getDate(),
                t.getDescription(),
                t.getCategory().getId(),
                t.getCategory().getName(),
                t.getGroup() != null ? t.getGroup().getId() : null,
                t.getGroup() != null ? t.getGroup().getName() : null,
                t.getPaidBy() != null ? t.getPaidBy().getId() : null
        );
    }
}
