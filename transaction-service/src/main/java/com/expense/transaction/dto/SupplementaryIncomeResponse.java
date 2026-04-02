package com.expense.transaction.dto;

import com.expense.transaction.entity.IncomeCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplementaryIncomeResponse {

    private Long id;
    private Long userId;
    private Double amount;
    private IncomeCategory category;
    private String description;
    private LocalDate incomeDate;
    private String paymentMethod;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
