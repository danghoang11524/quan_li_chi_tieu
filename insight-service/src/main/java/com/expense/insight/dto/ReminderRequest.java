package com.expense.insight.dto;

import com.expense.insight.entity.Reminder.ReminderFrequency;
import com.expense.insight.entity.Reminder.ReminderType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReminderRequest {
    private ReminderType type;
    private String title;
    private String description;
    private LocalTime reminderTime;
    private LocalDate dueDate;
    private BigDecimal amount;
    private ReminderFrequency frequency;
    private Boolean active;
}
