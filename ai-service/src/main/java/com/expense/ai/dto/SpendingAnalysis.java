package com.expense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingAnalysis {
    private Double totalIncome;
    private Double totalExpense;
    private Double savingsRate;
    private Map<String, Double> categoryBreakdown;
    private List<String> topSpendingCategories;
    private List<String> insights;
    private List<String> recommendations;
    private String financialHealth; // EXCELLENT, GOOD, MODERATE, POOR
}
