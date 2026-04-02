package com.expense.budget.dto;

import com.expense.budget.entity.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentalExpenseResponse {

    private Long id;
    private Long userId;
    private Double amount;
    private ExpenseCategory category;
    private String description;
    private LocalDate expenseDate;
    private String paymentMethod;
    private String tags;
    private String aiSuggestedCategory;
    private String aiSuggestedType;
    private Boolean aiSuggestionAccepted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
