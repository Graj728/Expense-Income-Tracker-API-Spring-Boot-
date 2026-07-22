package com.expensetracker.controller;

import com.expensetracker.dto.CategoryResponse;
import com.expensetracker.entity.enums.TransactionType;
import com.expensetracker.service.CategoryService;
import com.expensetracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(@RequestParam TransactionType type) {
        return ResponseEntity.ok(categoryService.listVisible(type, SecurityUtils.currentUserId()));
    }
}
