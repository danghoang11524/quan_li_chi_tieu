package com.expense.savings.service;

import com.expense.common.exception.BadRequestException;
import com.expense.common.exception.ResourceNotFoundException;
import com.expense.savings.dto.ContributionRequest;
import com.expense.savings.dto.SavingsGoalRequest;
import com.expense.savings.dto.SavingsGoalResponse;
import com.expense.savings.entity.GoalStatus;
import com.expense.savings.entity.SavingsGoal;
import com.expense.savings.repository.SavingsGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;

    @Transactional
    public SavingsGoalResponse createSavingsGoal(Long userId, SavingsGoalRequest request) {
        if (request.getTargetDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Target date must be in the future");
        }

        SavingsGoal savingsGoal = SavingsGoal.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .targetAmount(request.getTargetAmount())
                .currentAmount(0.0)
                .targetDate(request.getTargetDate())
                .startDate(LocalDate.now())
                .status(GoalStatus.ACTIVE)
                .category(request.getCategory())
                .build();

        savingsGoal = savingsGoalRepository.save(savingsGoal);
        return mapToResponse(savingsGoal);
    }

    public List<SavingsGoalResponse> getAllSavingsGoals(Long userId) {
        return savingsGoalRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<SavingsGoalResponse> getActiveSavingsGoals(Long userId) {
        return savingsGoalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SavingsGoalResponse getSavingsGoalById(Long userId, Long goalId) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));

        if (!goal.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to savings goal");
        }

        return mapToResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse updateSavingsGoal(Long userId, Long goalId, SavingsGoalRequest request) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));

        if (!goal.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to savings goal");
        }

        if (request.getTargetDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Target date must be in the future");
        }

        goal.setName(request.getName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setCategory(request.getCategory());

        goal = savingsGoalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse addContribution(Long userId, Long goalId, ContributionRequest request) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));

        if (!goal.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to savings goal");
        }

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BadRequestException("Cannot add contribution to inactive goal");
        }

        double newAmount = goal.getCurrentAmount() + request.getAmount();
        goal.setCurrentAmount(newAmount);

        // Check if goal is completed
        if (newAmount >= goal.getTargetAmount()) {
            goal.setStatus(GoalStatus.COMPLETED);
        }

        goal = savingsGoalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional
    public void deleteSavingsGoal(Long userId, Long goalId) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));

        if (!goal.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to savings goal");
        }

        savingsGoalRepository.delete(goal);
    }

    @Transactional
    public SavingsGoalResponse cancelSavingsGoal(Long userId, Long goalId) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));

        if (!goal.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to savings goal");
        }

        goal.setStatus(GoalStatus.CANCELLED);
        goal = savingsGoalRepository.save(goal);
        return mapToResponse(goal);
    }

    private SavingsGoalResponse mapToResponse(SavingsGoal goal) {
        double currentAmount = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : 0.0;
        double remainingAmount = goal.getTargetAmount() - currentAmount;
        double percentageCompleted = (currentAmount / goal.getTargetAmount()) * 100;

        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, goal.getTargetDate());

        // Calculate required daily, weekly, and monthly amounts
        double dailyRequired = 0.0;
        double weeklyRequired = 0.0;
        double monthlyRequired = 0.0;
        double currentMonthRequired = 0.0;

        if (daysRemaining > 0 && remainingAmount > 0) {
            dailyRequired = remainingAmount / daysRemaining;
            weeklyRequired = (remainingAmount / daysRemaining) * 7;
            monthlyRequired = (remainingAmount / daysRemaining) * 30;

            // Calculate savings needed from now until end of current month
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            long daysUntilEndOfMonth = ChronoUnit.DAYS.between(today, endOfMonth) + 1; // +1 to include today
            if (daysUntilEndOfMonth > 0) {
                currentMonthRequired = dailyRequired * daysUntilEndOfMonth;
            }
        }

        // Update status if overdue
        GoalStatus currentStatus = goal.getStatus();
        if (currentStatus == GoalStatus.ACTIVE && daysRemaining < 0) {
            currentStatus = GoalStatus.OVERDUE;
        }

        return SavingsGoalResponse.builder()
                .id(goal.getId())
                .userId(goal.getUserId())
                .name(goal.getName())
                .description(goal.getDescription())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(currentAmount)
                .remainingAmount(Math.max(0, remainingAmount))
                .percentageCompleted(Math.min(100, percentageCompleted))
                .targetDate(goal.getTargetDate())
                .startDate(goal.getStartDate())
                .status(currentStatus)
                .category(goal.getCategory())
                .daysRemaining((int) Math.max(0, daysRemaining))
                .dailyRequiredAmount(Math.max(0, dailyRequired))
                .weeklyRequiredAmount(Math.max(0, weeklyRequired))
                .monthlyRequiredAmount(Math.max(0, monthlyRequired))
                .currentMonthRequiredAmount(Math.max(0, currentMonthRequired))
                .build();
    }
}
