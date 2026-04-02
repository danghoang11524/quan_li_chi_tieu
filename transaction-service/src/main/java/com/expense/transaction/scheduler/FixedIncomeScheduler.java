package com.expense.transaction.scheduler;

import com.expense.transaction.service.FixedIncomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedIncomeScheduler {

    private final FixedIncomeService fixedIncomeService;

    /**
     * Automatically generates supplementary income entries from fixed income schedules
     * Runs every day at 00:01 AM
     */
    @Scheduled(cron = "0 1 0 * * ?")
    public void generateDueFixedIncome() {
        log.info("Starting scheduled fixed income generation...");
        try {
            fixedIncomeService.generateAllDueFixedIncome();
            log.info("Fixed income generation completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled fixed income generation", e);
        }
    }
}
