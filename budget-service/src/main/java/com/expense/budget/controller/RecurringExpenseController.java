package com.expense.budget.controller;

import com.expense.budget.dto.RecurringExpenseRequest;
import com.expense.budget.dto.RecurringExpenseResponse;
import com.expense.budget.service.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RecurringExpenseRequest request) {
        RecurringExpenseResponse response = recurringExpenseService.createRecurringExpense(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponse>> getAllRecurringExpenses(
            @RequestHeader("X-User-Id") Long userId) {
        List<RecurringExpenseResponse> expenses = recurringExpenseService.getAllRecurringExpenses(userId);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/active")
    public ResponseEntity<List<RecurringExpenseResponse>> getActiveRecurringExpenses(
            @RequestHeader("X-User-Id") Long userId) {
        List<RecurringExpenseResponse> expenses = recurringExpenseService.getActiveRecurringExpenses(userId);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> getRecurringExpenseById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        RecurringExpenseResponse response = recurringExpenseService.getRecurringExpenseById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> updateRecurringExpense(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody RecurringExpenseRequest request) {
        RecurringExpenseResponse response = recurringExpenseService.updateRecurringExpense(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        recurringExpenseService.deleteRecurringExpense(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RecurringExpenseResponse> toggleActiveStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        RecurringExpenseResponse response = recurringExpenseService.toggleActiveStatus(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<Void> generateIncidentalExpenseFromRecurring(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        recurringExpenseService.generateIncidentalExpenseFromRecurring(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate-all")
    public ResponseEntity<Void> generateAllDueRecurringExpenses() {
        recurringExpenseService.generateAllDueRecurringExpenses();
        return ResponseEntity.ok().build();
    }
}
