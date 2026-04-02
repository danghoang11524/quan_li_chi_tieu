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
public class PiggyBankDashboardResponse {
    // Income Summary
    private IncomeSummary incomeSummary;

    // Expense Summary
    private ExpenseSummary expenseSummary;

    // Overall Financial Health
    private FinancialOverview financialOverview;

    // AI Insights
    private AIInsights aiInsights;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomeSummary {
        // Fixed Income (Thu cố định)
        private Double totalFixedIncome;
        private Integer fixedIncomeCount;
        private List<FixedIncomeItem> activeFixedIncomes;

        // Supplementary Income (Thu phát sinh)
        private Double totalSupplementaryIncome;
        private Integer supplementaryIncomeCount;

        // Total
        private Double totalIncome;

        // Category Breakdown
        private Map<String, Double> incomeByCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseSummary {
        // Recurring Expenses (Chi cố định)
        private Double totalRecurringExpenses;
        private Integer recurringExpensesCount;
        private List<RecurringExpenseItem> activeRecurringExpenses;

        // Incidental Expenses (Chi phát sinh)
        private Double totalIncidentalExpenses;
        private Integer incidentalExpensesCount;

        // Total
        private Double totalExpenses;

        // Category Breakdown
        private Map<String, Double> expensesByCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FixedIncomeItem {
        private Long id;
        private String category;
        private Double amount;
        private String description;
        private String recurrencePattern;
        private String nextGenerationDate;
        private String generationStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecurringExpenseItem {
        private Long id;
        private String category;
        private Double amount;
        private String description;
        private String recurrencePattern;
        private String nextGenerationDate;
        private String generationStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialOverview {
        // Net Income (Thu - Chi)
        private Double netIncome;

        // Savings Rate (%)
        private Double savingsRate;

        // Financial Health Status
        private String financialHealth; // EXCELLENT, GOOD, AVERAGE, POOR

        // Monthly Trends
        private MonthlyTrend monthlyTrend;

        // Top Expense Categories
        private List<CategoryAmount> topExpenseCategories;

        // Budget Alerts
        private List<String> budgetAlerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private String currentMonth;
        private Double currentMonthIncome;
        private Double currentMonthExpenses;
        private Double currentMonthSavings;

        private String previousMonth;
        private Double previousMonthIncome;
        private Double previousMonthExpenses;
        private Double previousMonthSavings;

        private String trend; // IMPROVING, STABLE, DECLINING
        private String trendMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAmount {
        private String category;
        private Double amount;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIInsights {
        // AI Generated Suggestions
        private List<String> savingsTips;
        private List<String> expenseOptimizations;
        private List<String> incomeOpportunities;

        // Predictions
        private Double predictedNextMonthExpenses;
        private String spendingTrend; // INCREASING, STABLE, DECREASING

        // Anomalies
        private List<String> unusualTransactions;

        // Recommendations
        private String overallRecommendation;
        private String confidenceLevel; // HIGH, MEDIUM, LOW
    }
}
