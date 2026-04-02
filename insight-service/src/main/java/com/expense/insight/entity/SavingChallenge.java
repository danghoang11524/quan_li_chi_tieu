package com.expense.insight.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "saving_challenges")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeType type;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private Integer currentWeek;

    @Column(nullable = false)
    private Integer currentDay;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalSaved;

    @Column(precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalSaved == null) {
            totalSaved = BigDecimal.ZERO;
        }
        if (currentWeek == null) {
            currentWeek = 1;
        }
        if (currentDay == null) {
            currentDay = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ChallengeType {
        SEVEN_DAY,
        FIFTY_TWO_WEEK
    }

    public enum ChallengeStatus {
        ACTIVE,
        COMPLETED,
        FAILED,
        PAUSED
    }
}
