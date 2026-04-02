package com.expense.transaction.dto;

import com.expense.transaction.entity.IncomeCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplementaryIncomeRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Category is required")
    private IncomeCategory category;

    private String description;

    @NotNull(message = "Income date is required")
    private LocalDate incomeDate;

    private String paymentMethod;

    private String tags;
}
