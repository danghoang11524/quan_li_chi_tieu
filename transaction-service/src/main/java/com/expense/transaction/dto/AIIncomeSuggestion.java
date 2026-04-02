package com.expense.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIIncomeSuggestion {
    private String suggestedCategory; // SALARY, BONUS, INVESTMENT, BUSINESS, OTHER
    private String confidence; // HIGH, MEDIUM, LOW
    private String reasoning; // Explanation from AI in Vietnamese
}
