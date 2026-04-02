package com.expense.budget.dto;

import com.expense.budget.entity.ExpenseCategory;
import com.expense.budget.entity.IntervalUnit;
import com.expense.budget.entity.RecurrencePattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpenseResponse {

    private Long id;
    private Long userId;
    private Double amount;
    private ExpenseCategory category;
    private String description;
    private String paymentMethod;
    private String tags;
    private RecurrencePattern recurrencePattern;
    private LocalDate startDate;
    private LocalDate endDate;
    private String daysOfWeek;
    private String daysOfMonth;
    private Integer dayOfMonth;
    private Integer monthOfYear;
    private LocalDate biWeeklyReferenceDate;
    private Integer customIntervalValue;
    private IntervalUnit customIntervalUnit;
    private Boolean isActive;
    private LocalDate lastGeneratedDate;
    private LocalDate nextGenerationDate; // Calculated field
    private String generationStatus; // Calculated field
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
