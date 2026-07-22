package com.expensetracker.controller;

import com.expensetracker.dto.GroupExpenseRequest;
import com.expensetracker.dto.GroupExpenseResponse;
import com.expensetracker.service.SplitterService;
import com.expensetracker.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Group expense creation + the splitter logic (EQUAL / PERCENTAGE / EXACT).
 */
@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class GroupExpenseController {

    private final SplitterService splitterService;

    @PostMapping
    public ResponseEntity<GroupExpenseResponse> createExpense(@PathVariable Long groupId,
                                                                @Valid @RequestBody GroupExpenseRequest request) {
        GroupExpenseResponse response = splitterService.createGroupExpense(
                SecurityUtils.currentUserId(), groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupExpenseResponse>> listExpenses(@PathVariable Long groupId) {
        return ResponseEntity.ok(splitterService.listGroupExpenses(SecurityUtils.currentUserId(), groupId));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long groupId, @PathVariable Long transactionId) {
        splitterService.deleteGroupExpense(SecurityUtils.currentUserId(), groupId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
