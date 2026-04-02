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
public class SpendingPrediction {
    private Double predictedNextMonthExpense;
    private Map<String, Double> categoryPredictions;
    private Double confidenceLevel;
    private String trend; // INCREASING, STABLE, DECREASING
    private List<String> warnings;
}
