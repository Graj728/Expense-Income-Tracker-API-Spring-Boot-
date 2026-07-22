package com.expensetracker.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Net balances for every member of a group, plus the simplified list of
 * transfers required to settle all debts using the minimum number of payments.
 */
public record BalanceResponse(
        List<MemberBalance> netBalances,
        List<SuggestedTransfer> suggestedSettlements
) {
    /** Positive balance = this member is owed money overall. Negative = this member owes money overall. */
    public record MemberBalance(Long userId, String userName, BigDecimal netBalance) {}

    public record SuggestedTransfer(Long fromUserId, String fromUserName,
                                     Long toUserId, String toUserName,
                                     BigDecimal amount) {}
}
