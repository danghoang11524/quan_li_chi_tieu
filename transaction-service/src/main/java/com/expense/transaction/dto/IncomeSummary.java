package com.expense.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeSummary {

    private Double totalFixedIncome;
    private Double totalSupplementaryIncome;
    private Double totalIncome;
    private Long fixedIncomeCount;
    private Long supplementaryIncomeCount;
    private Long totalCount;
}
