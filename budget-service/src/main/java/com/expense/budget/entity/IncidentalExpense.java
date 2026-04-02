package com.expense.budget.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidental_expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentalExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    private String description;

    @Column(nullable = false)
    private LocalDate expenseDate;

    private String paymentMethod;

    private String tags;

    // AI-suggested fields (optional)
    private String aiSuggestedCategory; // Category suggested by AI
    private String aiSuggestedType; // "RECURRING" or "INCIDENTAL"
    private Boolean aiSuggestionAccepted; // Whether user accepted AI suggestion

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
