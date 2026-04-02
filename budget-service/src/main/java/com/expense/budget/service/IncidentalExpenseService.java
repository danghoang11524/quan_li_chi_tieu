package com.expense.budget.service;

import com.expense.budget.dto.ExpenseSummary;
import com.expense.budget.dto.IncidentalExpenseRequest;
import com.expense.budget.dto.IncidentalExpenseResponse;
import com.expense.budget.dto.AICategorySuggestion;
import com.expense.budget.entity.ExpenseCategory;
import com.expense.budget.entity.IncidentalExpense;
import com.expense.common.exception.ResourceNotFoundException;
import com.expense.budget.repository.IncidentalExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidentalExpenseService {

    private final IncidentalExpenseRepository incidentalExpenseRepository;
    private final AICategorizationService aiCategorizationService;

    @Transactional
    public IncidentalExpenseResponse createIncidentalExpense(Long userId, IncidentalExpenseRequest request) {
        IncidentalExpense expense = new IncidentalExpense();
        expense.setUserId(userId);
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setTags(request.getTags());
        expense.setAiSuggestedCategory(request.getAiSuggestedCategory());
        expense.setAiSuggestedType(request.getAiSuggestedType());
        expense.setAiSuggestionAccepted(request.getAiSuggestionAccepted());

        IncidentalExpense saved = incidentalExpenseRepository.save(expense);
        return mapToResponse(saved);
    }

    public List<IncidentalExpenseResponse> getAllIncidentalExpenses(Long userId) {
        List<IncidentalExpense> expenses = incidentalExpenseRepository.findByUserIdOrderByExpenseDateDesc(userId);
        return expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public IncidentalExpenseResponse getIncidentalExpenseById(Long userId, Long id) {
        IncidentalExpense expense = incidentalExpenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidental expense not found"));
        return mapToResponse(expense);
    }

    public List<IncidentalExpenseResponse> getIncidentalExpensesByDateRange(
            Long userId, LocalDate startDate, LocalDate endDate) {
        List<IncidentalExpense> expenses = incidentalExpenseRepository
                .findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(userId, startDate, endDate);
        return expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<IncidentalExpenseResponse> getIncidentalExpensesByCategory(
            Long userId, ExpenseCategory category) {
        List<IncidentalExpense> expenses = incidentalExpenseRepository
                .findByUserIdAndCategoryOrderByExpenseDateDesc(userId, category);
        return expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public IncidentalExpenseResponse updateIncidentalExpense(
            Long userId, Long id, IncidentalExpenseRequest request) {
        IncidentalExpense expense = incidentalExpenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidental expense not found"));

        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setTags(request.getTags());
        expense.setAiSuggestedCategory(request.getAiSuggestedCategory());
        expense.setAiSuggestedType(request.getAiSuggestedType());
        expense.setAiSuggestionAccepted(request.getAiSuggestionAccepted());

        IncidentalExpense updated = incidentalExpenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteIncidentalExpense(Long userId, Long id) {
        if (!incidentalExpenseRepository.findByIdAndUserId(id, userId).isPresent()) {
            throw new ResourceNotFoundException("Incidental expense not found");
        }
        incidentalExpenseRepository.deleteByIdAndUserId(id, userId);
    }

    public AICategorySuggestion getCategorySuggestion(String description) {
        return aiCategorizationService.categorizeExpense(description);
    }

    public ExpenseSummary getExpenseSummary(Long userId) {
        Double totalIncidental = incidentalExpenseRepository.calculateTotalByUserId(userId);
        Long incidentalCount = incidentalExpenseRepository.countByUserId(userId);

        ExpenseSummary summary = new ExpenseSummary();
        summary.setTotalIncidentalExpenses(totalIncidental != null ? totalIncidental : 0.0);
        summary.setIncidentalExpensesCount(incidentalCount);
        summary.setTotalRecurringExpenses(0.0);
        summary.setRecurringExpensesCount(0L);
        summary.setTotalExpenses(summary.getTotalIncidentalExpenses());
        summary.setTotalCount(incidentalCount);

        return summary;
    }

    public ExpenseSummary getExpenseSummaryByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        Double totalIncidental = incidentalExpenseRepository
                .calculateTotalByUserIdAndDateRange(userId, startDate, endDate);
        Long incidentalCount = incidentalExpenseRepository
                .countByUserIdAndExpenseDateBetween(userId, startDate, endDate);

        ExpenseSummary summary = new ExpenseSummary();
        summary.setTotalIncidentalExpenses(totalIncidental != null ? totalIncidental : 0.0);
        summary.setIncidentalExpensesCount(incidentalCount);
        summary.setTotalRecurringExpenses(0.0);
        summary.setRecurringExpensesCount(0L);
        summary.setTotalExpenses(summary.getTotalIncidentalExpenses());
        summary.setTotalCount(incidentalCount);

        return summary;
    }

    private IncidentalExpenseResponse mapToResponse(IncidentalExpense expense) {
        IncidentalExpenseResponse response = new IncidentalExpenseResponse();
        response.setId(expense.getId());
        response.setUserId(expense.getUserId());
        response.setAmount(expense.getAmount());
        response.setCategory(expense.getCategory());
        response.setDescription(expense.getDescription());
        response.setExpenseDate(expense.getExpenseDate());
        response.setPaymentMethod(expense.getPaymentMethod());
        response.setTags(expense.getTags());
        response.setAiSuggestedCategory(expense.getAiSuggestedCategory());
        response.setAiSuggestedType(expense.getAiSuggestedType());
        response.setAiSuggestionAccepted(expense.getAiSuggestionAccepted());
        response.setCreatedAt(expense.getCreatedAt());
        response.setUpdatedAt(expense.getUpdatedAt());
        return response;
    }
}
