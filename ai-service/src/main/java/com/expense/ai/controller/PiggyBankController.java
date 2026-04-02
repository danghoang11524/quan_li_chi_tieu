package com.expense.ai.controller;

import com.expense.ai.dto.PiggyBankDashboardResponse;
import com.expense.ai.service.PiggyBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho tính năng Heo Đất Ảo (Virtual Piggy Bank)
 * Tổng hợp dữ liệu thu nhập và chi phí để hiển thị dashboard và AI phân tích
 */
@Slf4j
@RestController
@RequestMapping("/api/piggy-bank")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PiggyBankController {

    private final PiggyBankService piggyBankService;

    /**
     * Lấy dashboard tổng hợp cho Heo Đất Ảo
     *
     * Endpoint này tổng hợp:
     * - Thu nhập: Thu cố định (Fixed Income) + Thu phát sinh (Supplementary Income)
     * - Chi phí: Chi cố định (Recurring Expenses) + Chi phát sinh (Incidental Expenses)
     * - Tổng quan tài chính: Thu nhập ròng, tỷ lệ tiết kiệm, sức khỏe tài chính
     * - AI Insights: Gợi ý tiết kiệm, tối ưu chi phí, cơ hội tăng thu nhập
     *
     * @param userId ID của người dùng
     * @return Dashboard tổng hợp với đầy đủ thông tin tài chính và AI insights
     */
    @GetMapping("/dashboard")
    public ResponseEntity<PiggyBankDashboardResponse> getDashboard(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("GET /api/piggy-bank/dashboard - User ID: {}", userId);

        try {
            PiggyBankDashboardResponse dashboard = piggyBankService.getDashboard(userId);

            log.info("Dashboard retrieved successfully for user {}", userId);
            log.debug("Dashboard summary - Total Income: {}, Total Expenses: {}, Net Income: {}",
                    dashboard.getIncomeSummary().getTotalIncome(),
                    dashboard.getExpenseSummary().getTotalExpenses(),
                    dashboard.getFinancialOverview().getNetIncome());

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("Error getting dashboard for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Piggy Bank Service is running!");
    }
}
