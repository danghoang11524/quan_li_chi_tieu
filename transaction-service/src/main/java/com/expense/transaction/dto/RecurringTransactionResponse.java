package com.expense.transaction.dto;

import com.expense.transaction.entity.RecurrencePattern;
import com.expense.transaction.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionResponse {

    private Long id;
    private Long userId;
    private TransactionType type;
    private Double amount;
    private String category;
    private String description;
    private String paymentMethod;
    private String tags;
    private RecurrencePattern recurrencePattern;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private Integer monthOfYear;
    private Boolean isActive;
    private LocalDate lastGeneratedDate;
    private LocalDate nextGenerationDate; // Sẽ được tính toán
    private LocalDateTime createdAt;
}
