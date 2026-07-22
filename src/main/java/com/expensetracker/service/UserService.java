package com.expensetracker.service;

import com.expensetracker.dto.UserSummary;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Looks up users by a partial, case-insensitive match on name or email.
     * Used to find people to add as group members without exposing raw IDs in the UI.
     * Requires at least 2 characters to avoid a full-table scan on a near-empty query.
     */
    @Transactional(readOnly = true)
    public List<UserSummary> search(String query, Long excludeUserId) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        String q = query.trim();
        return userRepository.findTop10ByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q).stream()
                .filter(u -> excludeUserId == null || !u.getId().equals(excludeUserId))
                .map(u -> new UserSummary(u.getId(), u.getName(), u.getEmail()))
                .toList();
    }
}
