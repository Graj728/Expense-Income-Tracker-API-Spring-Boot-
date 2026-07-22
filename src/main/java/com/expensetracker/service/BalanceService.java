package com.expensetracker.service;

import com.expensetracker.dto.BalanceResponse;
import com.expensetracker.entity.ExpenseSplit;
import com.expensetracker.entity.GroupMember;
import com.expensetracker.entity.Settlement;
import com.expensetracker.entity.User;
import com.expensetracker.repository.ExpenseSplitRepository;
import com.expensetracker.repository.GroupMemberRepository;
import com.expensetracker.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes each group member's net balance (positive = owed money, negative = owes money)
 * and simplifies outstanding debts into the minimum number of suggested transfers.
 */
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseSplitRepository expenseSplitRepository;
    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;

    @Transactional(readOnly = true)
    public BalanceResponse computeBalances(Long requesterId, Long groupId) {
        groupService.requireMember(groupId, requesterId);

        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();

        for (GroupMember gm : groupMemberRepository.findByGroupId(groupId)) {
            net.put(gm.getUser().getId(), BigDecimal.ZERO);
            names.put(gm.getUser().getId(), gm.getUser().getName());
        }

        // Every split represents money a participant owes for a group expense.
        // The payer effectively fronted the total, so the payer nets +share owed by others,
        // and each non-payer participant nets -their own share.
        List<ExpenseSplit> splits = expenseSplitRepository.findByTransactionGroupId(groupId);
        for (ExpenseSplit split : splits) {
            Long payerId = split.getTransaction().getPaidBy().getId();
            Long owerId = split.getUser().getId();
            BigDecimal share = split.getShareAmount();

            if (owerId.equals(payerId)) {
                continue; // the payer's own share nets to zero
            }
            net.merge(owerId, share.negate(), BigDecimal::add);
            net.merge(payerId, share, BigDecimal::add);
        }

        // Recorded settlements move balances back toward zero.
        for (Settlement s : settlementRepository.findByGroupIdOrderBySettledAtDesc(groupId)) {
            net.merge(s.getPaidBy().getId(), s.getAmount(), BigDecimal::add);
            net.merge(s.getPaidTo().getId(), s.getAmount().negate(), BigDecimal::add);
        }

        List<BalanceResponse.MemberBalance> memberBalances = net.entrySet().stream()
                .map(e -> new BalanceResponse.MemberBalance(
                        e.getKey(), names.get(e.getKey()), e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        List<BalanceResponse.SuggestedTransfer> transfers = simplifyDebts(net, names);

        return new BalanceResponse(memberBalances, transfers);
    }

    /**
     * Greedy debt-simplification: repeatedly match the biggest debtor with the biggest
     * creditor until all balances are settled. This minimizes the number of transactions
     * needed to clear the group's debts (a standard greedy approach for this problem).
     */
    private List<BalanceResponse.SuggestedTransfer> simplifyDebts(Map<Long, BigDecimal> net, Map<Long, String> names) {
        BigDecimal threshold = new BigDecimal("0.01");

        List<Map.Entry<Long, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<Long, BigDecimal>> debtors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> e : net.entrySet()) {
            if (e.getValue().compareTo(threshold) > 0) {
                creditors.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
            } else if (e.getValue().compareTo(threshold.negate()) < 0) {
                debtors.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().negate()));
            }
        }

        List<BalanceResponse.SuggestedTransfer> transfers = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            creditors.sort(Comparator.comparing((Map.Entry<Long, BigDecimal> e) -> e.getValue()).reversed());
            debtors.sort(Comparator.comparing((Map.Entry<Long, BigDecimal> e) -> e.getValue()).reversed());

            Map.Entry<Long, BigDecimal> topCreditor = creditors.get(0);
            Map.Entry<Long, BigDecimal> topDebtor = debtors.get(0);

            BigDecimal amount = topCreditor.getValue().min(topDebtor.getValue());

            transfers.add(new BalanceResponse.SuggestedTransfer(
                    topDebtor.getKey(), names.get(topDebtor.getKey()),
                    topCreditor.getKey(), names.get(topCreditor.getKey()),
                    amount.setScale(2, RoundingMode.HALF_UP)
            ));

            BigDecimal remainingCredit = topCreditor.getValue().subtract(amount);
            BigDecimal remainingDebt = topDebtor.getValue().subtract(amount);

            if (remainingCredit.compareTo(threshold) <= 0) {
                creditors.remove(0);
            } else {
                creditors.set(0, new java.util.AbstractMap.SimpleEntry<>(topCreditor.getKey(), remainingCredit));
            }

            if (remainingDebt.compareTo(threshold) <= 0) {
                debtors.remove(0);
            } else {
                debtors.set(0, new java.util.AbstractMap.SimpleEntry<>(topDebtor.getKey(), remainingDebt));
            }
        }

        return transfers;
    }
}
