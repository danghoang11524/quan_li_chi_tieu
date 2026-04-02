package com.expense.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummary {

    private Double totalRecurringExpenses;
    private Double totalIncidentalExpenses;
    private Double totalExpenses;
    private Long recurringExpensesCount;
    private Long incidentalExpensesCount;
    private Long totalCount;
}
