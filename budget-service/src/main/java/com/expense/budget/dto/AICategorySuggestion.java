package com.expense.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AICategorySuggestion {
    private String suggestedCategory; // DINING, TRANSPORTATION, etc.
    private String suggestedType; // RECURRING or INCIDENTAL
    private String confidence; // HIGH, MEDIUM, LOW
    private String reasoning; // Explanation from AI
}
