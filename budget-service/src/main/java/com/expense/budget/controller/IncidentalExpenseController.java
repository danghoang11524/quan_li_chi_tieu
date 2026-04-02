package com.expense.budget.controller;

import com.expense.budget.dto.*;
import com.expense.budget.entity.ExpenseCategory;
import com.expense.budget.service.IncidentalExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/incidental-expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IncidentalExpenseController {

    private final IncidentalExpenseService incidentalExpenseService;

    @PostMapping
    public ResponseEntity<IncidentalExpenseResponse> createIncidentalExpense(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody IncidentalExpenseRequest request) {
        IncidentalExpenseResponse response = incidentalExpenseService.createIncidentalExpense(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<IncidentalExpenseResponse>> getAllIncidentalExpenses(
            @RequestHeader("X-User-Id") Long userId) {
        List<IncidentalExpenseResponse> expenses = incidentalExpenseService.getAllIncidentalExpenses(userId);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentalExpenseResponse> getIncidentalExpenseById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        IncidentalExpenseResponse response = incidentalExpenseService.getIncidentalExpenseById(userId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<IncidentalExpenseResponse>> getIncidentalExpensesByDateRange(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<IncidentalExpenseResponse> expenses =
                incidentalExpenseService.getIncidentalExpensesByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<IncidentalExpenseResponse>> getIncidentalExpensesByCategory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable ExpenseCategory category) {
        List<IncidentalExpenseResponse> expenses =
                incidentalExpenseService.getIncidentalExpensesByCategory(userId, category);
        return ResponseEntity.ok(expenses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentalExpenseResponse> updateIncidentalExpense(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody IncidentalExpenseRequest request) {
        IncidentalExpenseResponse response =
                incidentalExpenseService.updateIncidentalExpense(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncidentalExpense(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        incidentalExpenseService.deleteIncidentalExpense(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ai-categorize")
    public ResponseEntity<AICategorySuggestion> getCategorySuggestion(
            @Valid @RequestBody AICategoryRequest request) {
        AICategorySuggestion suggestion = incidentalExpenseService.getCategorySuggestion(request.getDescription());
        return ResponseEntity.ok(suggestion);
    }

    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummary> getExpenseSummary(
            @RequestHeader("X-User-Id") Long userId) {
        ExpenseSummary summary = incidentalExpenseService.getExpenseSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/summary/date-range")
    public ResponseEntity<ExpenseSummary> getExpenseSummaryByDateRange(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ExpenseSummary summary =
                incidentalExpenseService.getExpenseSummaryByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}
