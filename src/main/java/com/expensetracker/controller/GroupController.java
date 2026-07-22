package com.expensetracker.controller;

import com.expensetracker.dto.AddMemberRequest;
import com.expensetracker.dto.GroupRequest;
import com.expensetracker.dto.GroupResponse;
import com.expensetracker.service.GroupService;
import com.expensetracker.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> listMine() {
        return ResponseEntity.ok(groupService.listForUser(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getGroup(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable Long id,
                                                     @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(groupService.addMember(SecurityUtils.currentUserId(), id, request.userId()));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(SecurityUtils.currentUserId(), id, userId);
        return ResponseEntity.noContent().build();
    }
}
