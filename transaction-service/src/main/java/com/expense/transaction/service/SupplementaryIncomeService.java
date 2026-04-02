package com.expense.transaction.service;

import com.expense.transaction.dto.IncomeSummary;
import com.expense.transaction.dto.SupplementaryIncomeRequest;
import com.expense.transaction.dto.SupplementaryIncomeResponse;
import com.expense.transaction.entity.IncomeCategory;
import com.expense.transaction.entity.SupplementaryIncome;
import com.expense.common.exception.ResourceNotFoundException;
import com.expense.transaction.repository.SupplementaryIncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplementaryIncomeService {

    private final SupplementaryIncomeRepository supplementaryIncomeRepository;

    @Transactional
    public SupplementaryIncomeResponse createSupplementaryIncome(Long userId, SupplementaryIncomeRequest request) {
        SupplementaryIncome income = new SupplementaryIncome();
        income.setUserId(userId);
        income.setAmount(request.getAmount());
        income.setCategory(request.getCategory());
        income.setDescription(request.getDescription());
        income.setIncomeDate(request.getIncomeDate());
        income.setPaymentMethod(request.getPaymentMethod());
        income.setTags(request.getTags());

        SupplementaryIncome saved = supplementaryIncomeRepository.save(income);
        return mapToResponse(saved);
    }

    public List<SupplementaryIncomeResponse> getAllSupplementaryIncome(Long userId) {
        List<SupplementaryIncome> incomes = supplementaryIncomeRepository.findByUserIdOrderByIncomeDateDesc(userId);
        return incomes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SupplementaryIncomeResponse getSupplementaryIncomeById(Long userId, Long id) {
        SupplementaryIncome income = supplementaryIncomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplementary income not found"));
        return mapToResponse(income);
    }

    public List<SupplementaryIncomeResponse> getSupplementaryIncomeByDateRange(
            Long userId, LocalDate startDate, LocalDate endDate) {
        List<SupplementaryIncome> incomes = supplementaryIncomeRepository
                .findByUserIdAndIncomeDateBetweenOrderByIncomeDateDesc(userId, startDate, endDate);
        return incomes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<SupplementaryIncomeResponse> getSupplementaryIncomeByCategory(
            Long userId, IncomeCategory category) {
        List<SupplementaryIncome> incomes = supplementaryIncomeRepository
                .findByUserIdAndCategoryOrderByIncomeDateDesc(userId, category);
        return incomes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplementaryIncomeResponse updateSupplementaryIncome(
            Long userId, Long id, SupplementaryIncomeRequest request) {
        SupplementaryIncome income = supplementaryIncomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplementary income not found"));

        income.setAmount(request.getAmount());
        income.setCategory(request.getCategory());
        income.setDescription(request.getDescription());
        income.setIncomeDate(request.getIncomeDate());
        income.setPaymentMethod(request.getPaymentMethod());
        income.setTags(request.getTags());

        SupplementaryIncome updated = supplementaryIncomeRepository.save(income);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteSupplementaryIncome(Long userId, Long id) {
        if (!supplementaryIncomeRepository.findByIdAndUserId(id, userId).isPresent()) {
            throw new ResourceNotFoundException("Supplementary income not found");
        }
        supplementaryIncomeRepository.deleteByIdAndUserId(id, userId);
    }

    public IncomeSummary getIncomeSummary(Long userId) {
        Double totalSupplementary = supplementaryIncomeRepository.calculateTotalByUserId(userId);
        Long supplementaryCount = supplementaryIncomeRepository.countByUserId(userId);

        IncomeSummary summary = new IncomeSummary();
        summary.setTotalSupplementaryIncome(totalSupplementary != null ? totalSupplementary : 0.0);
        summary.setSupplementaryIncomeCount(supplementaryCount);
        summary.setTotalFixedIncome(0.0);
        summary.setFixedIncomeCount(0L);
        summary.setTotalIncome(summary.getTotalSupplementaryIncome());
        summary.setTotalCount(supplementaryCount);

        return summary;
    }

    public IncomeSummary getIncomeSummaryByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        Double totalSupplementary = supplementaryIncomeRepository
                .calculateTotalByUserIdAndDateRange(userId, startDate, endDate);
        Long supplementaryCount = supplementaryIncomeRepository
                .countByUserIdAndIncomeDateBetween(userId, startDate, endDate);

        IncomeSummary summary = new IncomeSummary();
        summary.setTotalSupplementaryIncome(totalSupplementary != null ? totalSupplementary : 0.0);
        summary.setSupplementaryIncomeCount(supplementaryCount);
        summary.setTotalFixedIncome(0.0);
        summary.setFixedIncomeCount(0L);
        summary.setTotalIncome(summary.getTotalSupplementaryIncome());
        summary.setTotalCount(supplementaryCount);

        return summary;
    }

    private SupplementaryIncomeResponse mapToResponse(SupplementaryIncome income) {
        SupplementaryIncomeResponse response = new SupplementaryIncomeResponse();
        response.setId(income.getId());
        response.setUserId(income.getUserId());
        response.setAmount(income.getAmount());
        response.setCategory(income.getCategory());
        response.setDescription(income.getDescription());
        response.setIncomeDate(income.getIncomeDate());
        response.setPaymentMethod(income.getPaymentMethod());
        response.setTags(income.getTags());
        response.setCreatedAt(income.getCreatedAt());
        response.setUpdatedAt(income.getUpdatedAt());
        return response;
    }
}
