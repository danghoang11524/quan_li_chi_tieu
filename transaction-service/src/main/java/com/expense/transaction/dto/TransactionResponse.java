package com.expense.transaction.dto;

import com.expense.transaction.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private Long userId;
    private TransactionType type;
    private Double amount;
    private String category;
    private String description;
    private LocalDate transactionDate;
    private String paymentMethod;
    private String tags;
    private LocalDateTime createdAt;
}
