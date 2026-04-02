package com.expense.budget.dto;

import com.expense.budget.entity.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentalExpenseRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    private String description;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    private String paymentMethod;

    private String tags;

    // AI-suggested fields (optional, filled by frontend after AI analysis)
    private String aiSuggestedCategory;
    private String aiSuggestedType;
    private Boolean aiSuggestionAccepted;
}
