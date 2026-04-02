package com.expense.transaction.dto;

import com.expense.transaction.entity.RecurrencePattern;
import com.expense.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionRequest {

    @NotNull(message = "Loại giao dịch không được để trống")
    private TransactionType type;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private Double amount;

    @NotNull(message = "Danh mục không được để trống")
    private String category;

    private String description;

    private String paymentMethod;

    private String tags;

    @NotNull(message = "Chu kỳ lặp lại không được để trống")
    private RecurrencePattern recurrencePattern;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    private Integer dayOfWeek;

    private Integer dayOfMonth;

    private Integer monthOfYear;

    private Boolean isActive = true;
}
