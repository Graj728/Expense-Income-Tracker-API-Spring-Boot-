package com.expensetracker.controller;

import com.expensetracker.dto.BalanceResponse;
import com.expensetracker.service.BalanceService;
import com.expensetracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping
    public ResponseEntity<BalanceResponse> getBalances(@PathVariable Long groupId) {
        return ResponseEntity.ok(balanceService.computeBalances(SecurityUtils.currentUserId(), groupId));
    }
}
