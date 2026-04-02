package com.expense.savings.dto;

import com.expense.savings.entity.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGoalResponse {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Double targetAmount;
    private Double currentAmount;
    private Double remainingAmount;
    private Double percentageCompleted;
    private LocalDate targetDate;
    private LocalDate startDate;
    private GoalStatus status;
    private String category;
    private Integer daysRemaining;
    private Double dailyRequiredAmount;  // How much to save per day to reach goal
    private Double weeklyRequiredAmount; // How much to save per week to reach goal
    private Double monthlyRequiredAmount; // How much to save per month to reach goal
    private Double currentMonthRequiredAmount; // How much to save from now until end of current month
}
