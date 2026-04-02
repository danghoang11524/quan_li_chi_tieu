package com.expense.transaction.controller;

import com.expense.common.dto.ApiResponse;
import com.expense.transaction.dto.TransactionRequest;
import com.expense.transaction.dto.TransactionResponse;
import com.expense.transaction.dto.TransactionSummary;
import com.expense.transaction.entity.TransactionType;
import com.expense.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions(
            @RequestHeader("X-User-Id") Long userId) {
        List<TransactionResponse> transactions = transactionService.getAllTransactions(userId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByDateRange(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByType(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable TransactionType type) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByType(userId, type);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByCategory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String category) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByCategory(userId, category);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        TransactionResponse response = transactionService.getTransactionById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummary(
            @RequestHeader("X-User-Id") Long userId) {
        TransactionSummary summary = transactionService.getTransactionSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/summary/date-range")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummaryByDateRange(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        TransactionSummary summary = transactionService.getTransactionSummaryByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
