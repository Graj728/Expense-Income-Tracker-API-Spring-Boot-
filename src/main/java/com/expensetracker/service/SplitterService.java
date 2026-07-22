package com.expensetracker.service;

import com.expensetracker.dto.GroupExpenseRequest;
import com.expensetracker.dto.GroupExpenseResponse;
import com.expensetracker.dto.SplitShareRequest;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.ExpenseSplit;
import com.expensetracker.entity.Group;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.User;
import com.expensetracker.entity.enums.SplitType;
import com.expensetracker.entity.enums.TransactionType;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.ExpenseSplitRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SplitterService {

    private final TransactionRepository transactionRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final GroupService groupService;

    private static final BigDecimal PENNY = new BigDecimal("0.01");

    @Transactional
    public GroupExpenseResponse createGroupExpense(Long requesterId, Long groupId, GroupExpenseRequest request) {
        Group group = groupService.requireGroup(groupId);
        groupService.requireMember(groupId, requesterId);

        User paidBy = userRepository.findById(request.paidByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer user not found"));
        if (!groupService.isMemberSilent(groupId, paidBy.getId())) {
            throw new BadRequestException("Payer must be a member of the group");
        }

        Category category = categoryService.getById(request.categoryId());
        if (category.getType() != TransactionType.EXPENSE) {
            throw new BadRequestException("Group expenses must use an EXPENSE category");
        }

        Map<Long, BigDecimal> shareMap = computeShares(groupId, request);

        Transaction transaction = Transaction.builder()
                .type(TransactionType.EXPENSE)
                .amount(request.amount())
                .date(request.date())
                .description(request.description())
                .category(category)
                .user(paidBy)
                .group(group)
                .paidBy(paidBy)
                .build();
        transaction = transactionRepository.save(transaction);

        List<ExpenseSplit> splits = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : shareMap.entrySet()) {
            User participant = userRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Participant user not found: " + entry.getKey()));
            ExpenseSplit split = ExpenseSplit.builder()
                    .transaction(transaction)
                    .user(participant)
                    .shareAmount(entry.getValue())
                    // The payer's own share is implicitly covered since they fronted the money.
                    .paid(participant.getId().equals(paidBy.getId()))
                    .build();
            splits.add(expenseSplitRepository.save(split));
        }

        return toResponse(transaction, splits);
    }

    private Map<Long, BigDecimal> computeShares(Long groupId, GroupExpenseRequest request) {
        BigDecimal amount = request.amount();

        for (Long uid : participantsOrShareUserIds(request)) {
            if (!groupService.isMemberSilent(groupId, uid)) {
                throw new BadRequestException("User " + uid + " is not a member of this group");
            }
        }

        return switch (request.splitType()) {
            case EQUAL -> computeEqualSplit(amount, request.participantUserIds());
            case PERCENTAGE -> computePercentageSplit(amount, request.shares());
            case EXACT -> computeExactSplit(amount, request.shares());
        };
    }

    private List<Long> participantsOrShareUserIds(GroupExpenseRequest request) {
        if (request.splitType() == SplitType.EQUAL) {
            if (request.participantUserIds() == null || request.participantUserIds().isEmpty()) {
                throw new BadRequestException("participantUserIds is required for an EQUAL split");
            }
            return request.participantUserIds();
        }
        if (request.shares() == null || request.shares().isEmpty()) {
            throw new BadRequestException("shares is required for PERCENTAGE and EXACT splits");
        }
        return request.shares().stream().map(SplitShareRequest::userId).toList();
    }

    private Map<Long, BigDecimal> computeEqualSplit(BigDecimal amount, List<Long> participantUserIds) {
        int n = participantUserIds.size();
        BigDecimal base = amount.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal distributed = base.multiply(BigDecimal.valueOf(n));
        BigDecimal remainder = amount.subtract(distributed); // leftover pennies from rounding down

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        int remainderCents = remainder.divide(PENNY).intValue();

        for (int i = 0; i < n; i++) {
            BigDecimal share = base;
            // Distribute leftover pennies one-by-one to the first N participants so the total matches exactly.
            if (i < remainderCents) {
                share = share.add(PENNY);
            }
            result.put(participantUserIds.get(i), share);
        }
        return result;
    }

    private Map<Long, BigDecimal> computePercentageSplit(BigDecimal amount, List<SplitShareRequest> shares) {
        BigDecimal totalPercent = shares.stream()
                .map(SplitShareRequest::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercent.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new BadRequestException("Percentages must sum to exactly 100, got " + totalPercent);
        }

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal runningTotal = BigDecimal.ZERO;

        for (int i = 0; i < shares.size(); i++) {
            SplitShareRequest s = shares.get(i);
            BigDecimal share;
            if (i == shares.size() - 1) {
                // Last participant absorbs any rounding difference so shares sum exactly to `amount`.
                share = amount.subtract(runningTotal).setScale(2, RoundingMode.HALF_UP);
            } else {
                share = amount.multiply(s.value())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                runningTotal = runningTotal.add(share);
            }
            result.put(s.userId(), share);
        }
        return result;
    }

    private Map<Long, BigDecimal> computeExactSplit(BigDecimal amount, List<SplitShareRequest> shares) {
        BigDecimal total = shares.stream()
                .map(SplitShareRequest::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.setScale(2, RoundingMode.HALF_UP).compareTo(amount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BadRequestException("Exact shares (" + total + ") must sum to the transaction amount (" + amount + ")");
        }

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (SplitShareRequest s : shares) {
            result.put(s.userId(), s.value().setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<GroupExpenseResponse> listGroupExpenses(Long requesterId, Long groupId) {
        groupService.requireMember(groupId, requesterId);
        List<Transaction> transactions = transactionRepository.findByGroupIdOrderByDateDesc(groupId);
        return transactions.stream()
                .map(t -> toResponse(t, expenseSplitRepository.findByTransactionId(t.getId())))
                .toList();
    }

    @Transactional
    public void deleteGroupExpense(Long requesterId, Long groupId, Long transactionId) {
        groupService.requireMember(groupId, requesterId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (transaction.getGroup() == null || !transaction.getGroup().getId().equals(groupId)) {
            throw new BadRequestException("Expense does not belong to this group");
        }
        transactionRepository.delete(transaction);
    }

    private GroupExpenseResponse toResponse(Transaction t, List<ExpenseSplit> splits) {
        List<GroupExpenseResponse.SplitLine> lines = splits.stream()
                .map(s -> new GroupExpenseResponse.SplitLine(
                        s.getUser().getId(), s.getUser().getName(), s.getShareAmount(), s.isPaid()))
                .toList();

        return new GroupExpenseResponse(
                t.getId(), t.getAmount(), t.getDate(), t.getDescription(),
                t.getPaidBy() != null ? t.getPaidBy().getId() : null, lines
        );
    }
}
