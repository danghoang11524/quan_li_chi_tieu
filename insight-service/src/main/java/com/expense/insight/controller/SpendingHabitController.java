package com.expense.insight.controller;

import com.expense.insight.service.SpendingHabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/spending-habits")
@RequiredArgsConstructor
public class SpendingHabitController {

    private final SpendingHabitService spendingHabitService;

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSpendingHabitSuggestions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "Hanoi") String location) {
        List<String> suggestions = spendingHabitService.getSpendingHabitSuggestions(userId, location);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/personalized")
    public ResponseEntity<String> getPersonalizedSuggestion(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam BigDecimal monthlyIncome,
            @RequestParam BigDecimal monthlySpending) {
        String suggestion = spendingHabitService.getPersonalizedSuggestion(userId, monthlyIncome, monthlySpending);
        return ResponseEntity.ok(suggestion);
    }
}
