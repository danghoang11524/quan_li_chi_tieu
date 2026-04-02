package com.expense.budget.dto;

import com.expense.budget.entity.BudgetPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {
    private Long id;
    private Long userId;
    private String category;
    private Double amount;
    private Double spentAmount;
    private Double remainingAmount;
    private Double percentageUsed;
    private BudgetPeriod period;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double alertThreshold;
    private Boolean isActive;
    private Boolean isOverBudget;
    private Boolean shouldAlert;
}
