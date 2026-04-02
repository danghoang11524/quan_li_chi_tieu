package com.expense.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummary {
    private Double totalIncome;
    private Double totalExpense;
    private Double balance;
    private Long totalTransactions;
}
