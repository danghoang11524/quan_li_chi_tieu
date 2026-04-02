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
public class StatisticsResponse {
    private PredictionData prediction; // Next period prediction
    private List<BudgetSuggestion> budgetSuggestions; // Suggested budget allocations
    private List<String> insights; // AI insights
    private List<String> warnings; // Warnings based on trends

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionData {
        private String period; // "2024-12" for next month
        private Double predictedIncome;
        private Double predictedExpense;
        private Map<String, Double> categoryPredictions; // Predicted spending by category
        private Double savingsPotential; // How much can be saved
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetSuggestion {
        private String category;
        private Double suggestedAmount;
        private Double currentAverage;
        private String reason; // Why this amount is suggested
        private String priority; // "HIGH", "MEDIUM", "LOW"
    }
}
