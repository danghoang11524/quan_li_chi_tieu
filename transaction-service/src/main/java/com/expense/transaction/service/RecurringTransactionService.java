package com.expense.transaction.service;

import com.expense.transaction.dto.RecurringTransactionRequest;
import com.expense.transaction.dto.RecurringTransactionResponse;
import com.expense.transaction.entity.RecurringTransaction;
import com.expense.transaction.entity.RecurrencePattern;
import com.expense.transaction.entity.Transaction;
import com.expense.transaction.repository.RecurringTransactionRepository;
import com.expense.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(Long userId, RecurringTransactionRequest request) {
        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setUserId(userId);
        recurringTransaction.setType(request.getType());
        recurringTransaction.setAmount(request.getAmount());
        recurringTransaction.setCategory(request.getCategory());
        recurringTransaction.setDescription(request.getDescription());
        recurringTransaction.setPaymentMethod(request.getPaymentMethod());
        recurringTransaction.setTags(request.getTags());
        recurringTransaction.setRecurrencePattern(request.getRecurrencePattern());
        recurringTransaction.setStartDate(request.getStartDate());
        recurringTransaction.setEndDate(request.getEndDate());
        recurringTransaction.setDayOfWeek(request.getDayOfWeek());
        recurringTransaction.setDayOfMonth(request.getDayOfMonth());
        recurringTransaction.setMonthOfYear(request.getMonthOfYear());
        recurringTransaction.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getAllRecurringTransactions(Long userId) {
        return recurringTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getActiveRecurringTransactions(Long userId) {
        return recurringTransactionRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransactionById(Long userId, Long id) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch định kỳ"));

        if (!recurringTransaction.getUserId().equals(userId)) {
            throw new RuntimeException("Không có quyền truy cập giao dịch định kỳ này");
        }

        return convertToResponse(recurringTransaction);
    }

    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(Long userId, Long id, RecurringTransactionRequest request) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch định kỳ"));

        if (!recurringTransaction.getUserId().equals(userId)) {
            throw new RuntimeException("Không có quyền cập nhật giao dịch định kỳ này");
        }

        recurringTransaction.setType(request.getType());
        recurringTransaction.setAmount(request.getAmount());
        recurringTransaction.setCategory(request.getCategory());
        recurringTransaction.setDescription(request.getDescription());
        recurringTransaction.setPaymentMethod(request.getPaymentMethod());
        recurringTransaction.setTags(request.getTags());
        recurringTransaction.setRecurrencePattern(request.getRecurrencePattern());
        recurringTransaction.setStartDate(request.getStartDate());
        recurringTransaction.setEndDate(request.getEndDate());
        recurringTransaction.setDayOfWeek(request.getDayOfWeek());
        recurringTransaction.setDayOfMonth(request.getDayOfMonth());
        recurringTransaction.setMonthOfYear(request.getMonthOfYear());
        recurringTransaction.setIsActive(request.getIsActive());

        RecurringTransaction updated = recurringTransactionRepository.save(recurringTransaction);
        return convertToResponse(updated);
    }

    @Transactional
    public void deleteRecurringTransaction(Long userId, Long id) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch định kỳ"));

        if (!recurringTransaction.getUserId().equals(userId)) {
            throw new RuntimeException("Không có quyền xóa giao dịch định kỳ này");
        }

        recurringTransactionRepository.delete(recurringTransaction);
    }

    @Transactional
    public RecurringTransactionResponse toggleActiveStatus(Long userId, Long id) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch định kỳ"));

        if (!recurringTransaction.getUserId().equals(userId)) {
            throw new RuntimeException("Không có quyền thay đổi trạng thái giao dịch định kỳ này");
        }

        recurringTransaction.setIsActive(!recurringTransaction.getIsActive());
        RecurringTransaction updated = recurringTransactionRepository.save(recurringTransaction);
        return convertToResponse(updated);
    }

    public List<RecurringTransaction> getRecurringTransactionsDueForGeneration(LocalDate currentDate) {
        return recurringTransactionRepository.findActiveRecurringTransactionsDueForGeneration(currentDate);
    }

    public void updateLastGeneratedDate(Long id, LocalDate date) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch định kỳ"));
        recurringTransaction.setLastGeneratedDate(date);
        recurringTransactionRepository.save(recurringTransaction);
    }

    private RecurringTransactionResponse convertToResponse(RecurringTransaction entity) {
        RecurringTransactionResponse response = new RecurringTransactionResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUserId());
        response.setType(entity.getType());
        response.setAmount(entity.getAmount());
        response.setCategory(entity.getCategory());
        response.setDescription(entity.getDescription());
        response.setPaymentMethod(entity.getPaymentMethod());
        response.setTags(entity.getTags());
        response.setRecurrencePattern(entity.getRecurrencePattern());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setDayOfWeek(entity.getDayOfWeek());
        response.setDayOfMonth(entity.getDayOfMonth());
        response.setMonthOfYear(entity.getMonthOfYear());
        response.setIsActive(entity.getIsActive());
        response.setLastGeneratedDate(entity.getLastGeneratedDate());
        response.setNextGenerationDate(calculateNextGenerationDate(entity));
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    public LocalDate calculateNextGenerationDate(RecurringTransaction recurring) {
        LocalDate baseDate = recurring.getLastGeneratedDate() != null
            ? recurring.getLastGeneratedDate()
            : recurring.getStartDate();

        LocalDate nextDate = null;

        switch (recurring.getRecurrencePattern()) {
            case DAILY:
                nextDate = baseDate.plusDays(1);
                break;

            case WEEKLY:
                if (recurring.getDayOfWeek() != null) {
                    DayOfWeek targetDay = DayOfWeek.of(recurring.getDayOfWeek());
                    nextDate = baseDate.with(TemporalAdjusters.next(targetDay));
                } else {
                    nextDate = baseDate.plusWeeks(1);
                }
                break;

            case MONTHLY:
                if (recurring.getDayOfMonth() != null) {
                    nextDate = baseDate.plusMonths(1).withDayOfMonth(
                        Math.min(recurring.getDayOfMonth(), baseDate.plusMonths(1).lengthOfMonth())
                    );
                } else {
                    nextDate = baseDate.plusMonths(1);
                }
                break;

            case YEARLY:
                if (recurring.getMonthOfYear() != null && recurring.getDayOfMonth() != null) {
                    nextDate = baseDate.plusYears(1)
                        .withMonth(recurring.getMonthOfYear())
                        .withDayOfMonth(recurring.getDayOfMonth());
                } else {
                    nextDate = baseDate.plusYears(1);
                }
                break;
        }

        // Kiểm tra nếu nextDate vượt quá endDate
        if (nextDate != null && recurring.getEndDate() != null && nextDate.isAfter(recurring.getEndDate())) {
            return null;
        }

        return nextDate;
    }

    /**
     * Tự động tạo các giao dịch từ recurring transactions theo lịch
     * Method này sẽ được gọi bởi scheduled job
     */
    @Transactional
    public int generateTransactionsFromRecurring(LocalDate currentDate) {
        log.info("Bắt đầu tạo giao dịch tự động cho ngày: {}", currentDate);

        List<RecurringTransaction> dueTransactions = getRecurringTransactionsDueForGeneration(currentDate);
        List<Transaction> createdTransactions = new ArrayList<>();

        for (RecurringTransaction recurring : dueTransactions) {
            try {
                // Xác định ngày tạo giao dịch
                LocalDate transactionDate = recurring.getLastGeneratedDate() != null
                    ? calculateNextGenerationDate(recurring)
                    : recurring.getStartDate();

                if (transactionDate == null || transactionDate.isAfter(currentDate)) {
                    continue;
                }

                // Tạo transaction mới từ recurring transaction
                Transaction transaction = Transaction.builder()
                    .userId(recurring.getUserId())
                    .type(recurring.getType())
                    .amount(recurring.getAmount())
                    .category(recurring.getCategory())
                    .description(recurring.getDescription() + " (Tự động)")
                    .transactionDate(transactionDate)
                    .paymentMethod(recurring.getPaymentMethod())
                    .tags(recurring.getTags())
                    .build();

                Transaction saved = transactionRepository.save(transaction);
                createdTransactions.add(saved);

                // Cập nhật lastGeneratedDate
                updateLastGeneratedDate(recurring.getId(), transactionDate);

                log.info("Đã tạo giao dịch tự động: ID={}, User={}, Amount={}, Date={}",
                    saved.getId(), saved.getUserId(), saved.getAmount(), saved.getTransactionDate());

            } catch (Exception e) {
                log.error("Lỗi khi tạo giao dịch từ recurring ID={}: {}", recurring.getId(), e.getMessage());
            }
        }

        log.info("Hoàn thành tạo {} giao dịch tự động", createdTransactions.size());
        return createdTransactions.size();
    }

    /**
     * Tạo giao dịch thủ công từ một recurring transaction cụ thể
     */
    @Transactional
    public Transaction generateSingleTransactionFromRecurring(Long userId, Long recurringId) {
        RecurringTransaction recurring = recurringTransactionRepository.findById(recurringId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch định kỳ"));

        if (!recurring.getUserId().equals(userId)) {
            throw new RuntimeException("Không có quyền tạo giao dịch từ recurring này");
        }

        if (!recurring.getIsActive()) {
            throw new RuntimeException("Giao dịch định kỳ đã bị tạm dừng");
        }

        // Xác định ngày tạo giao dịch - sử dụng nextGenerationDate nếu có
        LocalDate transactionDate = recurring.getLastGeneratedDate() != null
            ? calculateNextGenerationDate(recurring)
            : recurring.getStartDate();

        // Nếu không có nextGenerationDate hợp lệ, sử dụng ngày hiện tại
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }

        Transaction transaction = Transaction.builder()
            .userId(recurring.getUserId())
            .type(recurring.getType())
            .amount(recurring.getAmount())
            .category(recurring.getCategory())
            .description(recurring.getDescription() + " (Thủ công)")
            .transactionDate(transactionDate)
            .paymentMethod(recurring.getPaymentMethod())
            .tags(recurring.getTags())
            .build();

        Transaction saved = transactionRepository.save(transaction);

        // CẬP NHẬT lastGeneratedDate để tính toán nextGenerationDate mới
        updateLastGeneratedDate(recurring.getId(), transactionDate);

        log.info("Đã tạo giao dịch thủ công từ recurring ID={}, transactionDate={}", recurringId, transactionDate);

        return saved;
    }
}
