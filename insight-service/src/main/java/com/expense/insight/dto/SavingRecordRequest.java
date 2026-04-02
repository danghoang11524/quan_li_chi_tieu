package com.expense.insight.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SavingRecordRequest {
    private BigDecimal amount;
}
