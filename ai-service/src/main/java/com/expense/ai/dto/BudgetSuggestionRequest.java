package com.expense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSuggestionRequest {

    private String newCategory;
    private Double newAmount;
    private String newPeriod;

    private List<ExistingBudget> existingBudgets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExistingBudget {
        private String category;
        private Double amount;
        private Double spentAmount;
        private String period;
    }
}
