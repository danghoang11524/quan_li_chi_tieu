package com.expense.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AICoachService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:AIzaSyAc4jAgd_WNOg2DiWY4daxzNu1kbvHVWuY}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent";

    // URLs to fetch user data (direct service-to-service calls, no /api prefix)
    private static final String TRANSACTION_SERVICE_URL = "http://transaction-service:8082";
    private static final String BUDGET_SERVICE_URL = "http://budget-service:8083";
    private static final String SAVINGS_SERVICE_URL = "http://savings-goal-service:8084";

    // Income endpoints
    private static final String FIXED_INCOME_URL = TRANSACTION_SERVICE_URL + "/fixed-income";
    private static final String SUPPLEMENTARY_INCOME_URL = TRANSACTION_SERVICE_URL + "/supplementary-income";

    // Expense endpoints
    private static final String RECURRING_EXPENSE_URL = BUDGET_SERVICE_URL + "/api/recurring-expenses";
    private static final String INCIDENTAL_EXPENSE_URL = BUDGET_SERVICE_URL + "/api/incidental-expenses";

    public String processChat(Long userId, String userMessage) {
        try {
            // 1. Fetch user's financial data
            String financialContext = fetchUserFinancialData(userId);

            // 2. Build prompt with context
            String prompt = buildPrompt(userMessage, financialContext);

            // 3. Call Gemini API
            String aiResponse = callGeminiAPI(prompt);

            return aiResponse;
        } catch (Exception e) {
            log.error("Error processing chat: {}", e.getMessage(), e);
            return "Xin lỗi, tôi gặp sự cố khi xử lý yêu cầu của bạn. Vui lòng thử lại.";
        }
    }

    private String fetchUserFinancialData(Long userId) {
        try {
            StringBuilder context = new StringBuilder();
            YearMonth currentMonth = YearMonth.now();
            String startDate = currentMonth.atDay(1).toString();
            String endDate = currentMonth.atEndOfMonth().toString();

            double totalIncome = 0;
            double totalExpense = 0;
            Map<String, Double> incomeByCategory = new HashMap<>();
            Map<String, Double> expenseByCategory = new HashMap<>();

            // 1. Fetch Fixed Income (Thu nhập cố định)
            try {
                String fixedIncomeJson = webClientBuilder.build()
                    .get()
                    .uri(FIXED_INCOME_URL)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (fixedIncomeJson != null) {
                    JsonNode incomes = objectMapper.readTree(fixedIncomeJson);

                    if (incomes.isArray()) {
                        for (JsonNode income : incomes) {
                            double amount = income.path("amount").asDouble();
                            String category = income.has("category") ? income.path("category").asText() : "Thu nhập cố định";
                            totalIncome += amount;
                            incomeByCategory.put(category, incomeByCategory.getOrDefault(category, 0.0) + amount);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch fixed income: {}", e.getMessage());
            }

            // 2. Fetch Supplementary Income (Thu nhập bổ sung)
            try {
                String suppIncomeUrl = String.format("%s/date-range?startDate=%s&endDate=%s",
                    SUPPLEMENTARY_INCOME_URL, startDate, endDate);

                String suppIncomeJson = webClientBuilder.build()
                    .get()
                    .uri(suppIncomeUrl)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (suppIncomeJson != null) {
                    JsonNode incomes = objectMapper.readTree(suppIncomeJson);

                    if (incomes.isArray()) {
                        for (JsonNode income : incomes) {
                            double amount = income.path("amount").asDouble();
                            String category = income.has("category") ? income.path("category").asText() : "Thu nhập bổ sung";
                            totalIncome += amount;
                            incomeByCategory.put(category, incomeByCategory.getOrDefault(category, 0.0) + amount);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch supplementary income: {}", e.getMessage());
            }

            // 3. Fetch Recurring Expenses (Chi tiêu định kỳ)
            try {
                String recurringExpenseJson = webClientBuilder.build()
                    .get()
                    .uri(RECURRING_EXPENSE_URL)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (recurringExpenseJson != null) {
                    JsonNode expenses = objectMapper.readTree(recurringExpenseJson);

                    if (expenses.isArray()) {
                        for (JsonNode expense : expenses) {
                            double amount = expense.path("amount").asDouble();
                            String category = expense.has("category") ? expense.path("category").asText() : "Chi tiêu định kỳ";
                            totalExpense += amount;
                            expenseByCategory.put(category, expenseByCategory.getOrDefault(category, 0.0) + amount);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch recurring expenses: {}", e.getMessage());
            }

            // 4. Fetch Incidental Expenses (Chi tiêu bất thường)
            try {
                String incidentalExpenseUrl = String.format("%s/date-range?startDate=%s&endDate=%s",
                    INCIDENTAL_EXPENSE_URL, startDate, endDate);

                String incidentalExpenseJson = webClientBuilder.build()
                    .get()
                    .uri(incidentalExpenseUrl)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (incidentalExpenseJson != null) {
                    JsonNode expenses = objectMapper.readTree(incidentalExpenseJson);

                    if (expenses.isArray()) {
                        for (JsonNode expense : expenses) {
                            double amount = expense.path("amount").asDouble();
                            String category = expense.has("category") ? expense.path("category").asText() : "Chi tiêu bất thường";
                            totalExpense += amount;
                            expenseByCategory.put(category, expenseByCategory.getOrDefault(category, 0.0) + amount);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch incidental expenses: {}", e.getMessage());
            }

            // 5. Fetch Savings Goals (Heo đất)
            try {
                String savingsJson = webClientBuilder.build()
                    .get()
                    .uri(SAVINGS_SERVICE_URL)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (savingsJson != null) {
                    JsonNode response = objectMapper.readTree(savingsJson);
                    JsonNode goals = response.path("data");

                    if (goals.isArray() && goals.size() > 0) {
                        context.append("\n**Heo đất (Mục tiêu tiết kiệm):**\n");
                        for (JsonNode goal : goals) {
                            String name = goal.path("name").asText();
                            double target = goal.path("targetAmount").asDouble();
                            double current = goal.has("currentAmount") ? goal.path("currentAmount").asDouble() : 0;
                            double percentage = target > 0 ? (current/target)*100 : 0;
                            context.append(String.format("- %s: %,.0f / %,.0f VNĐ (%.0f%%)\n",
                                name, current, target, percentage));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch savings goals: {}", e.getMessage());
            }

            // Debug log
            log.debug("Total income: {}, Total expense: {}", totalIncome, totalExpense);
            log.debug("Income categories: {}, Expense categories: {}", incomeByCategory.size(), expenseByCategory.size());

            // Build summary - rebuild context in proper order
            if (totalIncome > 0 || totalExpense > 0) {
                String savingsSection = context.toString(); // Save savings section
                StringBuilder newContext = new StringBuilder();

                // Header
                newContext.append(String.format("**Dữ liệu tài chính tháng %d/%d:**\n",
                    currentMonth.getMonthValue(), currentMonth.getYear()));

                // Summary
                newContext.append(String.format("- Tổng thu nhập: %,.0f VNĐ\n", totalIncome));
                newContext.append(String.format("- Tổng chi tiêu: %,.0f VNĐ\n", totalExpense));
                newContext.append(String.format("- Số tiền còn lại: %,.0f VNĐ\n", totalIncome - totalExpense));

                // Income by category
                if (!incomeByCategory.isEmpty()) {
                    newContext.append("\n**Thu nhập theo danh mục:**\n");
                    incomeByCategory.forEach((category, amount) ->
                        newContext.append(String.format("- %s: %,.0f VNĐ\n", category, amount))
                    );
                }

                // Expense by category
                if (!expenseByCategory.isEmpty()) {
                    newContext.append("\n**Chi tiêu theo danh mục:**\n");
                    expenseByCategory.forEach((category, amount) ->
                        newContext.append(String.format("- %s: %,.0f VNĐ\n", category, amount))
                    );
                }

                // Add back savings section
                if (savingsSection.length() > 0) {
                    newContext.append(savingsSection);
                }

                context = newContext;
            }

            if (context.length() == 0) {
                return "Người dùng chưa có dữ liệu tài chính.";
            }

            return context.toString();
        } catch (Exception e) {
            log.error("Error fetching financial data: {}", e.getMessage(), e);
            return "Không thể lấy dữ liệu tài chính của người dùng.";
        }
    }

    private String buildPrompt(String userMessage, String financialContext) {
        return String.format("""
            Bạn là AI Coach tài chính cá nhân thông minh và nhiệt tình. Nhiệm vụ của bạn là:
            1. Phân tích dữ liệu tài chính của người dùng
            2. Đưa ra lời khuyên cụ thể, thiết thực
            3. Trả lời bằng tiếng Việt, thân thiện và dễ hiểu
            4. Sử dụng emoji phù hợp để làm câu trả lời sinh động hơn

            %s

            Câu hỏi của người dùng: "%s"

            Hãy trả lời một cách ngắn gọn (3-5 câu), tập trung vào câu hỏi và dựa trên dữ liệu thực tế.
            Nếu không có đủ dữ liệu, hãy đưa ra lời khuyên chung về tài chính cá nhân.
            """, financialContext, userMessage);
    }

    private String callGeminiAPI(String prompt) {
        try {
            // Build request body for Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();

            parts.put("text", prompt);
            content.put("parts", new Object[]{parts});
            requestBody.put("contents", new Object[]{content});

            log.debug("Calling Gemini API with prompt length: {}", prompt.length());

            // Call Gemini API
            String responseJson = webClientBuilder.build()
                .post()
                .uri(GEMINI_API_URL + "?key=" + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.debug("Received response from Gemini API");

            // Parse response
            JsonNode response = objectMapper.readTree(responseJson);

            // Check if response has candidates
            if (!response.has("candidates") || response.get("candidates").size() == 0) {
                log.error("No candidates in Gemini response: {}", responseJson);
                return "Xin lỗi, AI không thể tạo phản hồi. Vui lòng thử lại.";
            }

            String aiResponse = response
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

            return aiResponse;

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);

            // Check if it's a quota error
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("quota"))) {
                return "⚠️ API đã hết quota sử dụng miễn phí. Vui lòng:\n" +
                       "1. Kiểm tra giới hạn API tại: https://ai.dev/usage\n" +
                       "2. Chờ giới hạn được reset (thường là hàng ngày)\n" +
                       "3. Hoặc nâng cấp lên gói trả phí\n\n" +
                       "Trong thời gian chờ đợi, bạn có thể xem các gợi ý tài chính ở tab 'Tips Tài Chính'.";
            }

            throw new RuntimeException("Failed to get response from AI", e);
        }
    }
}
