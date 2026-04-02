package com.expense.ai.service;

import com.expense.ai.dto.PiggyBankDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PiggyBankService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final GeminiService geminiService;

    private static final String TRANSACTION_SERVICE_URL = "http://transaction-service:8082";
    private static final String BUDGET_SERVICE_URL = "http://budget-service:8083";

    /**
     * Lấy dashboard tổng hợp cho heo đất ảo
     */
    public PiggyBankDashboardResponse getDashboard(Long userId) {
        log.info("Getting piggy bank dashboard for user {}", userId);

        try {
            // Parallel fetching data
            PiggyBankDashboardResponse.IncomeSummary incomeSummary = getIncomeSummary(userId);
            PiggyBankDashboardResponse.ExpenseSummary expenseSummary = getExpenseSummary(userId);

            // Calculate financial overview
            PiggyBankDashboardResponse.FinancialOverview financialOverview =
                calculateFinancialOverview(userId, incomeSummary, expenseSummary);

            // Get AI insights
            PiggyBankDashboardResponse.AIInsights aiInsights =
                generateAIInsights(userId, incomeSummary, expenseSummary, financialOverview);

            return PiggyBankDashboardResponse.builder()
                    .incomeSummary(incomeSummary)
                    .expenseSummary(expenseSummary)
                    .financialOverview(financialOverview)
                    .aiInsights(aiInsights)
                    .build();

        } catch (Exception e) {
            log.error("Error getting piggy bank dashboard for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get piggy bank dashboard: " + e.getMessage());
        }
    }

    /**
     * Lấy tổng hợp thu nhập
     */
    private PiggyBankDashboardResponse.IncomeSummary getIncomeSummary(Long userId) {
        try {
            // Create headers with X-User-Id
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            // Get Fixed Income
            String fixedIncomeUrl = TRANSACTION_SERVICE_URL + "/fixed-income?userId=" + userId;
            ResponseEntity<List<Map<String, Object>>> fixedIncomeResponse = restTemplate.exchange(
                    fixedIncomeUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> fixedIncomes = fixedIncomeResponse.getBody();

            // Get Supplementary Income for current month
            YearMonth currentMonth = YearMonth.now();
            LocalDate startDate = currentMonth.atDay(1);
            LocalDate endDate = currentMonth.atEndOfMonth();

            String suppIncomeUrl = TRANSACTION_SERVICE_URL + "/supplementary-income/date-range" +
                    "?userId=" + userId +
                    "&startDate=" + startDate +
                    "&endDate=" + endDate;
            ResponseEntity<List<Map<String, Object>>> suppIncomeResponse = restTemplate.exchange(
                    suppIncomeUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> suppIncomes = suppIncomeResponse.getBody();

            // Calculate totals
            double totalFixedIncome = calculateMonthlyTotal(fixedIncomes != null ? fixedIncomes : new ArrayList<>());
            double totalSuppIncome = calculateActualTotal(suppIncomes != null ? suppIncomes : new ArrayList<>());

            // Group by category
            Map<String, Double> incomeByCategory = groupByCategory(suppIncomes);

            // Get active fixed incomes
            List<PiggyBankDashboardResponse.FixedIncomeItem> activeFixedIncomes =
                    (fixedIncomes != null ? fixedIncomes : new ArrayList<Map<String, Object>>()).stream()
                    .filter(fi -> Boolean.TRUE.equals(fi.get("isActive")))
                    .map(this::mapToFixedIncomeItem)
                    .collect(Collectors.toList());

            return PiggyBankDashboardResponse.IncomeSummary.builder()
                    .totalFixedIncome(totalFixedIncome)
                    .fixedIncomeCount(fixedIncomes != null ? fixedIncomes.size() : 0)
                    .activeFixedIncomes(activeFixedIncomes)
                    .totalSupplementaryIncome(totalSuppIncome)
                    .supplementaryIncomeCount(suppIncomes != null ? suppIncomes.size() : 0)
                    .totalIncome(totalFixedIncome + totalSuppIncome)
                    .incomeByCategory(incomeByCategory)
                    .build();

        } catch (Exception e) {
            log.error("Error getting income summary: {}", e.getMessage(), e);
            return PiggyBankDashboardResponse.IncomeSummary.builder()
                    .totalFixedIncome(0.0)
                    .totalSupplementaryIncome(0.0)
                    .totalIncome(0.0)
                    .incomeByCategory(new HashMap<>())
                    .build();
        }
    }

    /**
     * Lấy tổng hợp chi phí
     */
    private PiggyBankDashboardResponse.ExpenseSummary getExpenseSummary(Long userId) {
        try {
            // Create headers with X-User-Id
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            // Get Recurring Expenses
            String recurringExpUrl = BUDGET_SERVICE_URL + "/recurring-expenses?userId=" + userId;
            ResponseEntity<List<Map<String, Object>>> recurringExpResponse = restTemplate.exchange(
                    recurringExpUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> recurringExpenses = recurringExpResponse.getBody();

            // Get Incidental Expenses for current month
            YearMonth currentMonth = YearMonth.now();
            LocalDate startDate = currentMonth.atDay(1);
            LocalDate endDate = currentMonth.atEndOfMonth();

            String incidentalExpUrl = BUDGET_SERVICE_URL + "/incidental-expenses/date-range" +
                    "?userId=" + userId +
                    "&startDate=" + startDate +
                    "&endDate=" + endDate;
            ResponseEntity<List<Map<String, Object>>> incidentalExpResponse = restTemplate.exchange(
                    incidentalExpUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> incidentalExpenses = incidentalExpResponse.getBody();

            // Calculate totals
            double totalRecurringExp = calculateMonthlyTotal(recurringExpenses != null ? recurringExpenses : new ArrayList<>());
            double totalIncidentalExp = calculateActualTotal(incidentalExpenses != null ? incidentalExpenses : new ArrayList<>());

            // Group by category
            Map<String, Double> expensesByCategory = groupByCategory(incidentalExpenses);

            // Get active recurring expenses
            List<PiggyBankDashboardResponse.RecurringExpenseItem> activeRecurringExpenses =
                    (recurringExpenses != null ? recurringExpenses : new ArrayList<Map<String, Object>>()).stream()
                    .filter(re -> Boolean.TRUE.equals(re.get("isActive")))
                    .map(this::mapToRecurringExpenseItem)
                    .collect(Collectors.toList());

            return PiggyBankDashboardResponse.ExpenseSummary.builder()
                    .totalRecurringExpenses(totalRecurringExp)
                    .recurringExpensesCount(recurringExpenses != null ? recurringExpenses.size() : 0)
                    .activeRecurringExpenses(activeRecurringExpenses)
                    .totalIncidentalExpenses(totalIncidentalExp)
                    .incidentalExpensesCount(incidentalExpenses != null ? incidentalExpenses.size() : 0)
                    .totalExpenses(totalRecurringExp + totalIncidentalExp)
                    .expensesByCategory(expensesByCategory)
                    .build();

        } catch (Exception e) {
            log.error("Error getting expense summary: {}", e.getMessage(), e);
            return PiggyBankDashboardResponse.ExpenseSummary.builder()
                    .totalRecurringExpenses(0.0)
                    .totalIncidentalExpenses(0.0)
                    .totalExpenses(0.0)
                    .expensesByCategory(new HashMap<>())
                    .build();
        }
    }

    /**
     * Tính toán tổng quan tài chính
     */
    private PiggyBankDashboardResponse.FinancialOverview calculateFinancialOverview(
            Long userId,
            PiggyBankDashboardResponse.IncomeSummary incomeSummary,
            PiggyBankDashboardResponse.ExpenseSummary expenseSummary) {

        double totalIncome = incomeSummary.getTotalIncome();
        double totalExpenses = expenseSummary.getTotalExpenses();
        double netIncome = totalIncome - totalExpenses;
        double savingsRate = totalIncome > 0 ? (netIncome / totalIncome) * 100 : 0.0;

        // Determine financial health
        String financialHealth;
        if (savingsRate >= 30) {
            financialHealth = "EXCELLENT";
        } else if (savingsRate >= 20) {
            financialHealth = "GOOD";
        } else if (savingsRate >= 10) {
            financialHealth = "AVERAGE";
        } else {
            financialHealth = "POOR";
        }

        // Get monthly trend
        PiggyBankDashboardResponse.MonthlyTrend monthlyTrend = getMonthlyTrend(userId);

        // Get top expense categories
        List<PiggyBankDashboardResponse.CategoryAmount> topExpenseCategories =
                getTopExpenseCategories(expenseSummary.getExpensesByCategory(), totalExpenses);

        // Generate budget alerts
        List<String> budgetAlerts = generateBudgetAlerts(netIncome, savingsRate, totalIncome, totalExpenses);

        return PiggyBankDashboardResponse.FinancialOverview.builder()
                .netIncome(netIncome)
                .savingsRate(savingsRate)
                .financialHealth(financialHealth)
                .monthlyTrend(monthlyTrend)
                .topExpenseCategories(topExpenseCategories)
                .budgetAlerts(budgetAlerts)
                .build();
    }

    /**
     * Lấy xu hướng theo tháng
     */
    private PiggyBankDashboardResponse.MonthlyTrend getMonthlyTrend(Long userId) {
        // Current month data already calculated above
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        // For simplicity, return basic trend
        // In production, you'd fetch actual previous month data
        return PiggyBankDashboardResponse.MonthlyTrend.builder()
                .currentMonth(currentMonth.toString())
                .currentMonthIncome(0.0)
                .currentMonthExpenses(0.0)
                .currentMonthSavings(0.0)
                .previousMonth(previousMonth.toString())
                .previousMonthIncome(0.0)
                .previousMonthExpenses(0.0)
                .previousMonthSavings(0.0)
                .trend("STABLE")
                .trendMessage("Chưa có đủ dữ liệu để phân tích xu hướng")
                .build();
    }

    /**
     * Lấy top danh mục chi tiêu
     */
    private List<PiggyBankDashboardResponse.CategoryAmount> getTopExpenseCategories(
            Map<String, Double> expensesByCategory, double totalExpenses) {

        return expensesByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> PiggyBankDashboardResponse.CategoryAmount.builder()
                        .category(entry.getKey())
                        .amount(entry.getValue())
                        .percentage(totalExpenses > 0 ? (entry.getValue() / totalExpenses) * 100 : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Tạo cảnh báo ngân sách
     */
    private List<String> generateBudgetAlerts(double netIncome, double savingsRate,
                                               double totalIncome, double totalExpenses) {
        List<String> alerts = new ArrayList<>();

        if (netIncome < 0) {
            alerts.add("⚠️ Chi tiêu vượt quá thu nhập! Cần cắt giảm chi phí ngay.");
        } else if (savingsRate < 10) {
            alerts.add("⚠️ Tỷ lệ tiết kiệm thấp (< 10%). Nên tiết kiệm ít nhất 20% thu nhập.");
        }

        if (totalIncome == 0) {
            alerts.add("ℹ️ Chưa có dữ liệu thu nhập. Hãy thêm thu nhập cố định hoặc phát sinh.");
        }

        if (totalExpenses > totalIncome * 0.8) {
            alerts.add("⚠️ Chi tiêu chiếm >80% thu nhập. Hãy cân nhắc cắt giảm chi phí không cần thiết.");
        }

        return alerts;
    }

    /**
     * Tạo AI insights
     */
    private PiggyBankDashboardResponse.AIInsights generateAIInsights(
            Long userId,
            PiggyBankDashboardResponse.IncomeSummary incomeSummary,
            PiggyBankDashboardResponse.ExpenseSummary expenseSummary,
            PiggyBankDashboardResponse.FinancialOverview financialOverview) {

        List<String> savingsTips = new ArrayList<>();
        List<String> expenseOptimizations = new ArrayList<>();
        List<String> incomeOpportunities = new ArrayList<>();
        String overallRecommendation = "";
        String confidenceLevel = "MEDIUM";

        try {
            // Build context for AI
            String context = buildFinancialContext(incomeSummary, expenseSummary, financialOverview);

            // Format numbers for display
            String incomeFormatted = String.format("%,.0f VND", incomeSummary.getTotalIncome());
            String expensesFormatted = String.format("%,.0f VND", expenseSummary.getTotalExpenses());
            String netIncomeFormatted = String.format("%,.0f VND", financialOverview.getNetIncome());

            // Log context being sent to AI
            log.info("Financial context for AI:\n{}", context);

            // Create personalized tips with actual financial data from detailed breakdown
            if (financialOverview.getNetIncome() > 0) {
                // User has positive net income - focus on savings
                double suggestedSavings = incomeSummary.getTotalIncome() * 0.2;
                savingsTips.add(String.format("Tiết kiệm 20%% thu nhập (%,.0f VND/tháng)", suggestedSavings));

                // Mention specific income sources
                if (incomeSummary.getActiveFixedIncomes() != null && !incomeSummary.getActiveFixedIncomes().isEmpty()) {
                    String incomeSource = incomeSummary.getActiveFixedIncomes().get(0).getDescription();
                    savingsTips.add(String.format("Tự động chuyển tiền tiết kiệm ngay khi nhận %s (%s)",
                            incomeSource, incomeFormatted));
                } else {
                    savingsTips.add(String.format("Với thu nhập %s, hãy tự động chuyển tiền khi nhận lương", incomeFormatted));
                }

                savingsTips.add(String.format("Mục tiêu quỹ khẩn cấp: %,.0f - %,.0f VND (3-6 tháng chi tiêu)",
                        expenseSummary.getTotalExpenses() * 3, expenseSummary.getTotalExpenses() * 6));
            } else {
                // User has deficit - focus on reducing expenses
                savingsTips.add("Thu nhập hiện tại thấp hơn chi phí - ưu tiên cắt giảm chi tiêu");
                savingsTips.add("Theo dõi chi tiết mọi khoản chi và loại bỏ chi phí không cần thiết");
                savingsTips.add("Tìm nguồn thu nhập thêm để cân bằng ngân sách");
            }

            // Expense optimizations with actual data and specific categories
            if (expenseSummary.getTotalExpenses() > 0) {
                expenseOptimizations.add(String.format("Tổng chi phí hiện tại: %s - xem xét giảm 10-15%%", expensesFormatted));

                // Analyze top expense categories
                if (financialOverview.getTopExpenseCategories() != null && !financialOverview.getTopExpenseCategories().isEmpty()) {
                    PiggyBankDashboardResponse.CategoryAmount topCategory = financialOverview.getTopExpenseCategories().get(0);
                    expenseOptimizations.add(String.format("Danh mục chi nhiều nhất là %s (%,.0f VND, %.1f%%) - tập trung tối ưu ở đây",
                            topCategory.getCategory(), topCategory.getAmount(), topCategory.getPercentage()));
                }

                // Mention specific recurring expenses if any
                if (expenseSummary.getActiveRecurringExpenses() != null && !expenseSummary.getActiveRecurringExpenses().isEmpty()) {
                    expenseOptimizations.add(String.format("Có %d khoản chi cố định - xem xét đàm phán hoặc tìm giải pháp rẻ hơn",
                            expenseSummary.getActiveRecurringExpenses().size()));
                } else {
                    expenseOptimizations.add("Giảm chi phát sinh - lập ngân sách hàng ngày và tuân thủ nghiêm");
                }
            } else {
                expenseOptimizations.add("Bạn chưa có chi phí nào được ghi nhận");
                expenseOptimizations.add("Hãy bắt đầu theo dõi mọi khoản chi để quản lý tốt hơn");
                expenseOptimizations.add("Tạo ngân sách chi tiêu hàng tháng dựa trên thu nhập");
            }

            // Income opportunities with breakdown
            if (incomeSummary.getTotalIncome() > 0) {
                incomeOpportunities.add(String.format("Thu nhập hiện tại: %s - tìm cách tăng 10-20%% thông qua tăng lương hoặc công việc thêm", incomeFormatted));

                // Mention income sources breakdown
                int fixedCount = incomeSummary.getActiveFixedIncomes() != null ? incomeSummary.getActiveFixedIncomes().size() : 0;
                int suppCount = incomeSummary.getSupplementaryIncomeCount() != null ? incomeSummary.getSupplementaryIncomeCount() : 0;
                if (fixedCount > 0 || suppCount > 0) {
                    incomeOpportunities.add(String.format("Hiện có %d nguồn thu cố định và %d thu phát sinh - tìm thêm nguồn thu mới",
                            fixedCount, suppCount));
                } else {
                    incomeOpportunities.add("Phát triển kỹ năng chuyên môn để có cơ hội thăng tiến");
                }
            } else {
                incomeOpportunities.add("Tìm nguồn thu nhập chính - làm việc toàn thời gian hoặc kinh doanh");
                incomeOpportunities.add("Xây dựng kỹ năng có thu nhập (lập trình, thiết kế, marketing...)");
            }

            // Overall recommendation with numbers
            String healthStatus = financialOverview.getFinancialHealth();
            overallRecommendation = String.format(
                    "Tình hình tài chính: %s | Thu nhập: %s | Chi phí: %s | Tiết kiệm: %s (%.1f%%). %s",
                    healthStatus,
                    incomeFormatted,
                    expensesFormatted,
                    netIncomeFormatted,
                    financialOverview.getSavingsRate(),
                    healthStatus.equals("EXCELLENT") || healthStatus.equals("GOOD")
                            ? "Hãy duy trì và tăng cường tiết kiệm."
                            : "Cần cải thiện bằng cách tăng thu hoặc giảm chi."
            );

            confidenceLevel = "HIGH";

        } catch (Exception e) {
            log.error("Error generating AI insights: {}", e.getMessage());
            savingsTips.add("Thiết lập mục tiêu tiết kiệm hàng tháng");
            expenseOptimizations.add("Theo dõi chi tiêu hàng ngày");
            incomeOpportunities.add("Tìm hiểu các nguồn thu nhập thụ động");
            overallRecommendation = "Hãy bắt đầu bằng việc theo dõi chi tiêu và thiết lập ngân sách.";
        }

        return PiggyBankDashboardResponse.AIInsights.builder()
                .savingsTips(savingsTips)
                .expenseOptimizations(expenseOptimizations)
                .incomeOpportunities(incomeOpportunities)
                .predictedNextMonthExpenses(expenseSummary.getTotalExpenses() * 1.05) // +5% estimation
                .spendingTrend("STABLE")
                .unusualTransactions(new ArrayList<>())
                .overallRecommendation(overallRecommendation)
                .confidenceLevel(confidenceLevel)
                .build();
    }

    /**
     * Xây dựng context cho AI
     */
    private String buildFinancialContext(
            PiggyBankDashboardResponse.IncomeSummary incomeSummary,
            PiggyBankDashboardResponse.ExpenseSummary expenseSummary,
            PiggyBankDashboardResponse.FinancialOverview financialOverview) {

        StringBuilder context = new StringBuilder();

        // Header - Tổng quan
        context.append("=== TỔNG QUAN TÀI CHÍNH ===\n");
        context.append(String.format("Thu nhập: %,.0f VND | Chi phí: %,.0f VND | Tiết kiệm: %,.0f VND (%.1f%%)\n",
                incomeSummary.getTotalIncome(),
                expenseSummary.getTotalExpenses(),
                financialOverview.getNetIncome(),
                financialOverview.getSavingsRate()));
        context.append(String.format("Sức khỏe tài chính: %s\n\n", financialOverview.getFinancialHealth()));

        // Chi tiết Thu Nhập Cố Định
        context.append("=== THU NHẬP CỐ ĐỊNH (hàng tháng) ===\n");
        if (incomeSummary.getActiveFixedIncomes() != null && !incomeSummary.getActiveFixedIncomes().isEmpty()) {
            for (PiggyBankDashboardResponse.FixedIncomeItem item : incomeSummary.getActiveFixedIncomes()) {
                context.append(String.format("- %s: %,.0f VND (%s) - %s\n",
                        item.getCategory(),
                        item.getAmount(),
                        item.getRecurrencePattern(),
                        item.getDescription()));
            }
            context.append(String.format("Tổng: %,.0f VND/tháng\n\n", incomeSummary.getTotalFixedIncome()));
        } else {
            context.append("Chưa có thu nhập cố định\n\n");
        }

        // Thu nhập phát sinh theo danh mục
        context.append("=== THU NHẬP PHÁT SINH (tháng này) ===\n");
        if (incomeSummary.getIncomeByCategory() != null && !incomeSummary.getIncomeByCategory().isEmpty()) {
            incomeSummary.getIncomeByCategory().forEach((category, amount) -> {
                context.append(String.format("- %s: %,.0f VND\n", category, amount));
            });
            context.append(String.format("Tổng: %,.0f VND\n\n", incomeSummary.getTotalSupplementaryIncome()));
        } else {
            context.append("Chưa có thu nhập phát sinh\n\n");
        }

        // Chi tiết Chi Phí Cố Định
        context.append("=== CHI PHÍ CỐ ĐỊNH (hàng tháng) ===\n");
        if (expenseSummary.getActiveRecurringExpenses() != null && !expenseSummary.getActiveRecurringExpenses().isEmpty()) {
            for (PiggyBankDashboardResponse.RecurringExpenseItem item : expenseSummary.getActiveRecurringExpenses()) {
                context.append(String.format("- %s: %,.0f VND (%s) - %s\n",
                        item.getCategory(),
                        item.getAmount(),
                        item.getRecurrencePattern(),
                        item.getDescription()));
            }
            context.append(String.format("Tổng: %,.0f VND/tháng\n\n", expenseSummary.getTotalRecurringExpenses()));
        } else {
            context.append("Chưa có chi phí cố định\n\n");
        }

        // Chi phí phát sinh theo danh mục
        context.append("=== CHI PHÍ PHÁT SINH (tháng này) ===\n");
        if (expenseSummary.getExpensesByCategory() != null && !expenseSummary.getExpensesByCategory().isEmpty()) {
            expenseSummary.getExpensesByCategory().forEach((category, amount) -> {
                context.append(String.format("- %s: %,.0f VND\n", category, amount));
            });
            context.append(String.format("Tổng: %,.0f VND\n\n", expenseSummary.getTotalIncidentalExpenses()));
        } else {
            context.append("Chưa có chi phí phát sinh\n\n");
        }

        // Top chi tiêu
        if (financialOverview.getTopExpenseCategories() != null && !financialOverview.getTopExpenseCategories().isEmpty()) {
            context.append("=== TOP DANH MỤC CHI TIÊU ===\n");
            for (PiggyBankDashboardResponse.CategoryAmount cat : financialOverview.getTopExpenseCategories()) {
                context.append(String.format("- %s: %,.0f VND (%.1f%%)\n",
                        cat.getCategory(), cat.getAmount(), cat.getPercentage()));
            }
            context.append("\n");
        }

        return context.toString();
    }

    // Helper methods

    private double calculateMonthlyTotal(List<Map<String, Object>> items) {
        return items.stream()
                .mapToDouble(item -> {
                    Object amountObj = item.get("amount");
                    double amount = amountObj instanceof Integer ?
                            ((Integer) amountObj).doubleValue() :
                            (Double) amountObj;

                    String pattern = (String) item.getOrDefault("recurrencePattern", "MONTHLY");

                    // Convert to monthly
                    switch (pattern) {
                        case "DAILY": return amount * 30;
                        case "WEEKLY": return amount * 4.33;
                        case "BI_WEEKLY": return amount * 2.17;
                        case "MONTHLY": return amount;
                        case "YEARLY": return amount / 12;
                        default: return amount;
                    }
                })
                .sum();
    }

    private double calculateActualTotal(List<Map<String, Object>> items) {
        if (items == null) return 0.0;
        return items.stream()
                .mapToDouble(item -> {
                    Object amountObj = item.get("amount");
                    return amountObj instanceof Integer ?
                            ((Integer) amountObj).doubleValue() :
                            (Double) amountObj;
                })
                .sum();
    }

    private Map<String, Double> groupByCategory(List<Map<String, Object>> items) {
        if (items == null) return new HashMap<>();

        return items.stream()
                .collect(Collectors.groupingBy(
                        item -> (String) item.getOrDefault("category", "OTHER"),
                        Collectors.summingDouble(item -> {
                            Object amountObj = item.get("amount");
                            return amountObj instanceof Integer ?
                                    ((Integer) amountObj).doubleValue() :
                                    (Double) amountObj;
                        })
                ));
    }

    private PiggyBankDashboardResponse.FixedIncomeItem mapToFixedIncomeItem(Map<String, Object> item) {
        return PiggyBankDashboardResponse.FixedIncomeItem.builder()
                .id(((Number) item.get("id")).longValue())
                .category((String) item.getOrDefault("category", "OTHER"))
                .amount(item.get("amount") instanceof Integer ?
                        ((Integer) item.get("amount")).doubleValue() :
                        (Double) item.get("amount"))
                .description((String) item.getOrDefault("description", ""))
                .recurrencePattern((String) item.getOrDefault("recurrencePattern", "MONTHLY"))
                .nextGenerationDate((String) item.getOrDefault("nextGenerationDate", ""))
                .generationStatus((String) item.getOrDefault("generationStatus", ""))
                .build();
    }

    private PiggyBankDashboardResponse.RecurringExpenseItem mapToRecurringExpenseItem(Map<String, Object> item) {
        return PiggyBankDashboardResponse.RecurringExpenseItem.builder()
                .id(((Number) item.get("id")).longValue())
                .category((String) item.getOrDefault("category", "OTHER"))
                .amount(item.get("amount") instanceof Integer ?
                        ((Integer) item.get("amount")).doubleValue() :
                        (Double) item.get("amount"))
                .description((String) item.getOrDefault("description", ""))
                .recurrencePattern((String) item.getOrDefault("recurrencePattern", "MONTHLY"))
                .nextGenerationDate((String) item.getOrDefault("nextGenerationDate", ""))
                .generationStatus((String) item.getOrDefault("generationStatus", ""))
                .build();
    }
}
