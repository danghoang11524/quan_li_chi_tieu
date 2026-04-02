package com.expense.savings.controller;

import com.expense.common.dto.ApiResponse;
import com.expense.savings.dto.ContributionRequest;
import com.expense.savings.dto.SavingsGoalRequest;
import com.expense.savings.dto.SavingsGoalResponse;
import com.expense.savings.service.SavingsGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> createSavingsGoal(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SavingsGoalRequest request) {
        SavingsGoalResponse response = savingsGoalService.createSavingsGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Savings goal created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingsGoalResponse>>> getAllSavingsGoals(
            @RequestHeader("X-User-Id") Long userId) {
        List<SavingsGoalResponse> goals = savingsGoalService.getAllSavingsGoals(userId);
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SavingsGoalResponse>>> getActiveSavingsGoals(
            @RequestHeader("X-User-Id") Long userId) {
        List<SavingsGoalResponse> goals = savingsGoalService.getActiveSavingsGoals(userId);
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> getSavingsGoalById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        SavingsGoalResponse response = savingsGoalService.getSavingsGoalById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> updateSavingsGoal(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalRequest request) {
        SavingsGoalResponse response = savingsGoalService.updateSavingsGoal(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Savings goal updated successfully", response));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> addContribution(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ContributionRequest request) {
        SavingsGoalResponse response = savingsGoalService.addContribution(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Contribution added successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSavingsGoal(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        savingsGoalService.deleteSavingsGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Savings goal deleted successfully", null));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> cancelSavingsGoal(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        SavingsGoalResponse response = savingsGoalService.cancelSavingsGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Savings goal cancelled", response));
    }
}
