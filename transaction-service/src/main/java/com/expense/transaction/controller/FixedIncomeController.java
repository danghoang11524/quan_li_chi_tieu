package com.expense.transaction.controller;

import com.expense.transaction.dto.AIIncomeSuggestion;
import com.expense.transaction.dto.FixedIncomeRequest;
import com.expense.transaction.dto.FixedIncomeResponse;
import com.expense.transaction.service.AIIncomeCategorizationService;
import com.expense.transaction.service.FixedIncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fixed-income")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FixedIncomeController {

    private final FixedIncomeService fixedIncomeService;
    private final AIIncomeCategorizationService aiIncomeCategorizationService;

    @PostMapping
    public ResponseEntity<FixedIncomeResponse> createFixedIncome(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FixedIncomeRequest request) {
        FixedIncomeResponse response = fixedIncomeService.createFixedIncome(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FixedIncomeResponse>> getAllFixedIncome(
            @RequestHeader("X-User-Id") Long userId) {
        List<FixedIncomeResponse> incomes = fixedIncomeService.getAllFixedIncome(userId);
        return ResponseEntity.ok(incomes);
    }

    @GetMapping("/active")
    public ResponseEntity<List<FixedIncomeResponse>> getActiveFixedIncome(
            @RequestHeader("X-User-Id") Long userId) {
        List<FixedIncomeResponse> incomes = fixedIncomeService.getActiveFixedIncome(userId);
        return ResponseEntity.ok(incomes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FixedIncomeResponse> getFixedIncomeById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        FixedIncomeResponse response = fixedIncomeService.getFixedIncomeById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FixedIncomeResponse> updateFixedIncome(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody FixedIncomeRequest request) {
        FixedIncomeResponse response = fixedIncomeService.updateFixedIncome(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFixedIncome(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        fixedIncomeService.deleteFixedIncome(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<FixedIncomeResponse> toggleActiveStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        FixedIncomeResponse response = fixedIncomeService.toggleActiveStatus(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<Void> generateSupplementaryIncomeFromFixed(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        fixedIncomeService.generateSupplementaryIncomeFromFixed(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate-all")
    public ResponseEntity<Void> generateAllDueFixedIncome() {
        fixedIncomeService.generateAllDueFixedIncome();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ai-categorize")
    public ResponseEntity<AIIncomeSuggestion> categorizeIncome(@RequestBody Map<String, String> request) {
        String description = request.get("description");
        AIIncomeSuggestion suggestion = aiIncomeCategorizationService.categorizeIncome(description);
        return ResponseEntity.ok(suggestion);
    }
}
