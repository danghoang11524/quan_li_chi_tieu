package com.expense.insight.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_tips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipCategory category;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Boolean active;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (priority == null) {
            priority = 5;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TipCategory {
        SAVING,
        INVESTING,
        DEBT_MANAGEMENT,
        BUDGETING,
        EMERGENCY_FUND,
        INSURANCE,
        TAX,
        SHOPPING,
        GENERAL
    }
}
