package com.expense.transaction.controller;

import com.expense.transaction.dto.IncomeSummary;
import com.expense.transaction.dto.SupplementaryIncomeRequest;
import com.expense.transaction.dto.SupplementaryIncomeResponse;
import com.expense.transaction.entity.IncomeCategory;
import com.expense.transaction.service.SupplementaryIncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/supplementary-income")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplementaryIncomeController {

    private final SupplementaryIncomeService supplementaryIncomeService;

    @PostMapping
    public ResponseEntity<SupplementaryIncomeResponse> createSupplementaryIncome(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SupplementaryIncomeRequest request) {
        SupplementaryIncomeResponse response = supplementaryIncomeService.createSupplementaryIncome(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SupplementaryIncomeResponse>> getAllSupplementaryIncome(
            @RequestHeader("X-User-Id") Long userId) {
        List<SupplementaryIncomeResponse> incomes = supplementaryIncomeService.getAllSupplementaryIncome(userId);
        return ResponseEntity.ok(incomes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplementaryIncomeResponse> getSupplementaryIncomeById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        SupplementaryIncomeResponse response = supplementaryIncomeService.getSupplementaryIncomeById(userId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<SupplementaryIncomeResponse>> getSupplementaryIncomeByDateRange(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<SupplementaryIncomeResponse> incomes =
                supplementaryIncomeService.getSupplementaryIncomeByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(incomes);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<SupplementaryIncomeResponse>> getSupplementaryIncomeByCategory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable IncomeCategory category) {
        List<SupplementaryIncomeResponse> incomes =
                supplementaryIncomeService.getSupplementaryIncomeByCategory(userId, category);
        return ResponseEntity.ok(incomes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplementaryIncomeResponse> updateSupplementaryIncome(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody SupplementaryIncomeRequest request) {
        SupplementaryIncomeResponse response =
                supplementaryIncomeService.updateSupplementaryIncome(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplementaryIncome(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        supplementaryIncomeService.deleteSupplementaryIncome(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<IncomeSummary> getIncomeSummary(
            @RequestHeader("X-User-Id") Long userId) {
        IncomeSummary summary = supplementaryIncomeService.getIncomeSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/summary/date-range")
    public ResponseEntity<IncomeSummary> getIncomeSummaryByDateRange(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        IncomeSummary summary =
                supplementaryIncomeService.getIncomeSummaryByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}
