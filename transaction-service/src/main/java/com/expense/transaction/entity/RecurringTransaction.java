package com.expense.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String category;

    private String description;

    private String paymentMethod;

    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrencePattern recurrencePattern;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate; // Null = vô thời hạn

    // Cho WEEKLY: 1=Monday, 2=Tuesday, ..., 7=Sunday
    private Integer dayOfWeek;

    // Cho MONTHLY/YEARLY: ngày trong tháng (1-31)
    private Integer dayOfMonth;

    // Cho YEARLY: tháng trong năm (1-12)
    private Integer monthOfYear;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDate lastGeneratedDate; // Ngày giao dịch cuối cùng được tạo

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
