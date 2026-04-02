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
public class StatisticsRequest {
    private List<MonthlyData> monthlyData; // Historical data for past months
    private String period; // "week", "month", or "year"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyData {
        private String month; // e.g., "2024-01"
        private Double totalIncome;
        private Double totalExpense;
        private Map<String, Double> categoryExpenses; // Category -> Amount
    }
}
