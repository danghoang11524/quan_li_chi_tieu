package com.expense.transaction.controller;

import com.expense.common.dto.ApiResponse;
import com.expense.transaction.dto.RecurringTransactionRequest;
import com.expense.transaction.dto.RecurringTransactionResponse;
import com.expense.transaction.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> createRecurringTransaction(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.createRecurringTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo giao dịch định kỳ thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponse>>> getAllRecurringTransactions(
            @RequestHeader("X-User-Id") Long userId) {
        List<RecurringTransactionResponse> transactions = recurringTransactionService.getAllRecurringTransactions(userId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponse>>> getActiveRecurringTransactions(
            @RequestHeader("X-User-Id") Long userId) {
        List<RecurringTransactionResponse> transactions = recurringTransactionService.getActiveRecurringTransactions(userId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> getRecurringTransactionById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        RecurringTransactionResponse response = recurringTransactionService.getRecurringTransactionById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> updateRecurringTransaction(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.updateRecurringTransaction(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giao dịch định kỳ thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecurringTransaction(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        recurringTransactionService.deleteRecurringTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa giao dịch định kỳ thành công", null));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> toggleActiveStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        RecurringTransactionResponse response = recurringTransactionService.toggleActiveStatus(userId, id);
        String message = response.getIsActive()
            ? "Đã bật giao dịch định kỳ"
            : "Đã tạm dừng giao dịch định kỳ";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<ApiResponse<Object>> generateTransactionFromRecurring(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        var transaction = recurringTransactionService.generateSingleTransactionFromRecurring(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo giao dịch từ mẫu định kỳ", transaction));
    }

    @PostMapping("/generate-all")
    public ResponseEntity<ApiResponse<Object>> generateAllDueTransactions(
            @RequestHeader("X-User-Id") Long userId) {
        int count = recurringTransactionService.generateTransactionsFromRecurring(java.time.LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(
            "Đã tạo " + count + " giao dịch tự động từ các mẫu định kỳ",
            java.util.Map.of("generatedCount", count)
        ));
    }
}
