package com.expense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String category; // GENERAL_ADVICE, BUDGET_SUGGESTION, SAVINGS_TIP, EXPENSE_ANALYSIS
}
