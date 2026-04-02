package com.expense.budget.dto;

import com.expense.budget.entity.ExpenseCategory;
import com.expense.budget.entity.IntervalUnit;
import com.expense.budget.entity.RecurrencePattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpenseRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    private String description;

    private String paymentMethod;

    private String tags;

    @NotNull(message = "Recurrence pattern is required")
    private RecurrencePattern recurrencePattern;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    // For WEEKLY: comma-separated days (e.g., "1,3,5" for Mon, Wed, Fri)
    private String daysOfWeek;

    // For MONTHLY: comma-separated days (e.g., "1,15" for 1st and 15th)
    private String daysOfMonth;

    // For YEARLY
    private Integer dayOfMonth;
    private Integer monthOfYear;

    // For BI_WEEKLY
    private LocalDate biWeeklyReferenceDate;

    // For CUSTOM_INTERVAL
    private Integer customIntervalValue;
    private IntervalUnit customIntervalUnit;

    private Boolean isActive = true;
}
