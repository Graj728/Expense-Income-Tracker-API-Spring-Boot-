package com.expensetracker.controller;

import com.expensetracker.dto.UserSummary;
import com.expensetracker.service.UserService;
import com.expensetracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Searches users by partial name or email match, for adding group members.
     * Excludes the requesting user from their own results.
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSummary>> search(@RequestParam String query) {
        return ResponseEntity.ok(userService.search(query, SecurityUtils.currentUserId()));
    }
}
