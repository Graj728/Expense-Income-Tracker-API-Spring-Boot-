package com.expensetracker.controller;

import com.expensetracker.dto.SettlementRequest;
import com.expensetracker.dto.SettlementResponse;
import com.expensetracker.service.SettlementService;
import com.expensetracker.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<SettlementResponse> record(@PathVariable Long groupId,
                                                        @Valid @RequestBody SettlementRequest request) {
        SettlementResponse response = settlementService.recordSettlement(
                SecurityUtils.currentUserId(), groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SettlementResponse>> list(@PathVariable Long groupId) {
        return ResponseEntity.ok(settlementService.listForGroup(SecurityUtils.currentUserId(), groupId));
    }
}
