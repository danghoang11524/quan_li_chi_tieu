package com.expense.transaction.scheduler;

import com.expense.transaction.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduler tự động tạo giao dịch từ recurring transactions
 * Chạy mỗi ngày lúc 00:01 AM
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;

    /**
     * Tự động tạo giao dịch mỗi ngày lúc 00:01 AM
     * Cron format: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void generateDailyRecurringTransactions() {
        log.info("=== BẮT ĐẦU SCHEDULED JOB: Tạo giao dịch định kỳ tự động ===");

        try {
            LocalDate today = LocalDate.now();
            int count = recurringTransactionService.generateTransactionsFromRecurring(today);

            log.info("=== KẾT THÚC SCHEDULED JOB: Đã tạo {} giao dịch tự động ===", count);
        } catch (Exception e) {
            log.error("=== LỖI SCHEDULED JOB: {} ===", e.getMessage(), e);
        }
    }

    /**
     * Job chạy thử nghiệm mỗi 5 phút (để test)
     * Uncomment dòng @Scheduled bên dưới nếu muốn test job
     */
    // @Scheduled(fixedDelay = 300000) // 5 phút
    public void testGenerateRecurringTransactions() {
        log.info("=== TEST JOB: Tạo giao dịch định kỳ ===");

        try {
            LocalDate today = LocalDate.now();
            int count = recurringTransactionService.generateTransactionsFromRecurring(today);

            log.info("=== TEST JOB HOÀN THÀNH: Đã tạo {} giao dịch ===", count);
        } catch (Exception e) {
            log.error("=== LỖI TEST JOB: {} ===", e.getMessage(), e);
        }
    }
}
