package com.expense.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_income")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixedIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncomeCategory category;

    private String description;

    private String paymentMethod;

    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrencePattern recurrencePattern;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate; // Null = indefinite

    // For WEEKLY: 1=Monday, 2=Tuesday, ..., 7=Sunday (can be comma-separated for multiple days)
    private String daysOfWeek;

    // For MONTHLY: day(s) in month (1-31, can be comma-separated for multiple days like "1,15")
    private String daysOfMonth;

    // For YEARLY: day of month (1-31)
    private Integer dayOfMonth;

    // For YEARLY: month of year (1-12)
    private Integer monthOfYear;

    // For BI_WEEKLY: reference date to calculate bi-weekly schedule
    private LocalDate biWeeklyReferenceDate;

    // For CUSTOM_INTERVAL: interval value (e.g., 3 for "every 3 days")
    private Integer customIntervalValue;

    // For CUSTOM_INTERVAL: interval unit (DAYS, WEEKS, MONTHS)
    @Enumerated(EnumType.STRING)
    private IntervalUnit customIntervalUnit;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDate lastGeneratedDate; // Last date when income was auto-generated

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
