package com.expense.insight.controller;

import com.expense.insight.entity.FinancialTip;
import com.expense.insight.service.FinancialTipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tips")
@RequiredArgsConstructor
public class FinancialTipController {

    private final FinancialTipService financialTipService;

    @GetMapping
    public ResponseEntity<List<FinancialTip>> getAllTips() {
        return ResponseEntity.ok(financialTipService.getAllActiveTips());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FinancialTip>> getTipsByCategory(
            @PathVariable FinancialTip.TipCategory category) {
        return ResponseEntity.ok(financialTipService.getTipsByCategory(category));
    }

    @GetMapping("/random")
    public ResponseEntity<FinancialTip> getRandomTip() {
        return ResponseEntity.ok(financialTipService.getRandomTip());
    }

    @GetMapping("/daily")
    public ResponseEntity<List<FinancialTip>> getDailyTips(
            @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(financialTipService.getDailyTips(count));
    }
}
