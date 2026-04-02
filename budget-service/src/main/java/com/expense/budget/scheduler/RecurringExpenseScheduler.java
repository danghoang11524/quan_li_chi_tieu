package com.expense.budget.scheduler;

import com.expense.budget.service.RecurringExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseScheduler {

    private final RecurringExpenseService recurringExpenseService;

    /**
     * Automatically generates incidental expense entries from recurring expense schedules
     * Runs every day at 00:01 AM
     */
    @Scheduled(cron = "0 1 0 * * ?")
    public void generateDueRecurringExpenses() {
        log.info("Starting scheduled recurring expense generation...");
        try {
            recurringExpenseService.generateAllDueRecurringExpenses();
            log.info("Recurring expense generation completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled recurring expense generation", e);
        }
    }
}
