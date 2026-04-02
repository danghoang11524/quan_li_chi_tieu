package com.expense.transaction.service;

import com.expense.common.exception.BadRequestException;
import com.expense.common.exception.ResourceNotFoundException;
import com.expense.transaction.dto.TransactionRequest;
import com.expense.transaction.dto.TransactionResponse;
import com.expense.transaction.dto.TransactionSummary;
import com.expense.transaction.entity.Transaction;
import com.expense.transaction.entity.TransactionType;
import com.expense.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .type(request.getType())
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .paymentMethod(request.getPaymentMethod())
                .tags(request.getTags())
                .build();

        transaction = transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    public List<TransactionResponse> getAllTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByType(Long userId, TransactionType type) {
        return transactionRepository.findByUserIdAndTypeOrderByTransactionDateDesc(userId, type)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByCategory(Long userId, String category) {
        return transactionRepository.findByUserIdAndCategoryOrderByTransactionDateDesc(userId, category)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to transaction");
        }

        return mapToResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to transaction");
        }

        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setTags(request.getTags());

        transaction = transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to transaction");
        }

        transactionRepository.delete(transaction);
    }

    public TransactionSummary getTransactionSummary(Long userId) {
        Double totalIncome = transactionRepository.sumByUserIdAndType(userId, TransactionType.INCOME);
        Double totalExpense = transactionRepository.sumByUserIdAndType(userId, TransactionType.EXPENSE);
        Long totalTransactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId).stream().count();

        totalIncome = totalIncome != null ? totalIncome : 0.0;
        totalExpense = totalExpense != null ? totalExpense : 0.0;

        return TransactionSummary.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome - totalExpense)
                .totalTransactions(totalTransactions)
                .build();
    }

    public TransactionSummary getTransactionSummaryByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        Double totalIncome = transactionRepository.sumByUserIdAndTypeAndDateRange(userId, TransactionType.INCOME, startDate, endDate);
        Double totalExpense = transactionRepository.sumByUserIdAndTypeAndDateRange(userId, TransactionType.EXPENSE, startDate, endDate);
        Long totalTransactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, startDate, endDate)
                .stream()
                .count();

        totalIncome = totalIncome != null ? totalIncome : 0.0;
        totalExpense = totalExpense != null ? totalExpense : 0.0;

        return TransactionSummary.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome - totalExpense)
                .totalTransactions(totalTransactions)
                .build();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .paymentMethod(transaction.getPaymentMethod())
                .tags(transaction.getTags())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
