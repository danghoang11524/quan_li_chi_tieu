package com.expense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSuggestionResponse {

    private Boolean shouldCreate;
    private String recommendation;
    private String reason;
    private Double suggestedAmount;
    private String[] warnings;
    private String[] tips;
}
