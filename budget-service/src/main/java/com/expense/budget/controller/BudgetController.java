package com.expense.budget.controller;

import com.expense.budget.dto.BudgetRequest;
import com.expense.budget.dto.BudgetResponse;
import com.expense.budget.service.BudgetService;
import com.expense.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.createBudget(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget created successfully", response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getActiveBudgets(
            @RequestHeader("X-User-Id") Long userId) {
        List<BudgetResponse> budgets = budgetService.getActiveBudgets(userId);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getAllBudgets(
            @RequestHeader("X-User-Id") Long userId) {
        List<BudgetResponse> budgets = budgetService.getAllBudgets(userId);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        BudgetResponse response = budgetService.getBudgetById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.updateBudget(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully", null));
    }

    @PostMapping("/ai-suggestion")
    public ResponseEntity<ApiResponse<String>> getAISuggestion(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody java.util.Map<String, Object> request) {
        String suggestion = budgetService.getAISuggestion(userId, request);
        return ResponseEntity.ok(ApiResponse.success("AI suggestion generated successfully", suggestion));
    }

    @PostMapping("/check-before-create")
    public ResponseEntity<ApiResponse<String>> checkBudgetBeforeCreate(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody java.util.Map<String, Object> request) {
        String analysis = budgetService.checkBudgetBeforeCreate(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Budget analysis completed", analysis));
    }
}
