package com.expense.budget.service;

import com.expense.budget.dto.BudgetRequest;
import com.expense.budget.dto.BudgetResponse;
import com.expense.budget.entity.Budget;
import com.expense.budget.repository.BudgetRepository;
import com.expense.common.exception.BadRequestException;
import com.expense.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final WebClient.Builder webClientBuilder;

    @Transactional
    public BudgetResponse createBudget(Long userId, BudgetRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        Budget budget = Budget.builder()
                .userId(userId)
                .category(request.getCategory())
                .amount(request.getAmount())
                .spentAmount(0.0)
                .period(request.getPeriod())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .alertThreshold(request.getAlertThreshold() != null ? request.getAlertThreshold() : 80.0)
                .isActive(true)
                .build();

        budget = budgetRepository.save(budget);
        return mapToResponse(budget);
    }

    public List<BudgetResponse> getActiveBudgets(Long userId) {
        return budgetRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BudgetResponse> getAllBudgets(Long userId) {
        return budgetRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BudgetResponse getBudgetById(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to budget");
        }

        return mapToResponse(budget);
    }

    @Transactional
    public BudgetResponse updateBudget(Long userId, Long budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to budget");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        budget.setCategory(request.getCategory());
        budget.setAmount(request.getAmount());
        budget.setPeriod(request.getPeriod());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setAlertThreshold(request.getAlertThreshold());

        budget = budgetRepository.save(budget);
        return mapToResponse(budget);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to budget");
        }

        budgetRepository.delete(budget);
    }

    @Transactional
    public void updateSpentAmount(Long budgetId, Double spentAmount) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budget.setSpentAmount(spentAmount);
        budgetRepository.save(budget);
    }

    private BudgetResponse mapToResponse(Budget budget) {
        // Calculate spent amount from transactions for this budget period and category
        double spentAmount = calculateSpentAmount(budget);

        double remainingAmount = budget.getAmount() - spentAmount;
        double percentageUsed = budget.getAmount() > 0 ? (spentAmount / budget.getAmount()) * 100 : 0.0;
        boolean isOverBudget = spentAmount > budget.getAmount();
        boolean shouldAlert = percentageUsed >= budget.getAlertThreshold();

        return BudgetResponse.builder()
                .id(budget.getId())
                .userId(budget.getUserId())
                .category(budget.getCategory())
                .amount(budget.getAmount())
                .spentAmount(spentAmount)
                .remainingAmount(remainingAmount)
                .percentageUsed(percentageUsed)
                .period(budget.getPeriod())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .alertThreshold(budget.getAlertThreshold())
                .isActive(budget.getIsActive())
                .isOverBudget(isOverBudget)
                .shouldAlert(shouldAlert)
                .build();
    }

    /**
     * Check budget before creation with comprehensive AI analysis
     * Includes recurring income, existing budgets, and spending history
     */
    public String checkBudgetBeforeCreate(Long userId, Map<String, Object> request) {
        try {
            // Get budget details from request
            String newCategory = (String) request.get("category");
            Object newAmountObj = request.get("amount");
            Double newAmount = newAmountObj instanceof Integer ? ((Integer) newAmountObj).doubleValue() : (Double) newAmountObj;
            String newPeriod = (String) request.get("period");
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");

            // Get existing budgets
            List<Budget> existingBudgets = budgetRepository.findByUserIdAndIsActiveTrue(userId);

            // Get recurring incomes (thu nhập cố định)
            List<Map<String, Object>> recurringIncomes = new java.util.ArrayList<>();
            try {
                String recurringUrl = "http://transaction-service:8082/recurring/active";
                ApiResponseWrapper<List<Map<String, Object>>> recurringResponse = webClientBuilder.build()
                    .get()
                    .uri(recurringUrl)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponseWrapper<List<Map<String, Object>>>>() {})
                    .block();

                if (recurringResponse != null && recurringResponse.getData() != null) {
                    // Filter only INCOME types
                    recurringIncomes = recurringResponse.getData().stream()
                        .filter(r -> "INCOME".equals(r.get("type")))
                        .collect(Collectors.toList());
                }
            } catch (Exception e) {
                System.err.println("ERROR fetching recurring incomes: " + e.getMessage());
            }

            // Get spending history for last 30 days
            Map<String, Object> spendingHistory = new HashMap<>();
            try {
                java.time.LocalDate now = java.time.LocalDate.now();
                java.time.LocalDate thirtyDaysAgo = now.minusDays(30);

                String transactionUrl = String.format(
                    "http://transaction-service:8082/date-range?startDate=%s&endDate=%s",
                    thirtyDaysAgo, now
                );

                ApiResponseWrapper<List<TransactionDTO>> transactionResponse = webClientBuilder.build()
                    .get()
                    .uri(transactionUrl)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponseWrapper<List<TransactionDTO>>>() {})
                    .block();

                if (transactionResponse != null && transactionResponse.getData() != null) {
                    List<TransactionDTO> transactions = transactionResponse.getData();

                    // Calculate total spent
                    double totalSpent = transactions.stream()
                        .filter(t -> "EXPENSE".equals(t.getType()))
                        .mapToDouble(TransactionDTO::getAmount)
                        .sum();

                    // Calculate by category
                    Map<String, Double> byCategory = transactions.stream()
                        .filter(t -> "EXPENSE".equals(t.getType()))
                        .collect(Collectors.groupingBy(
                            TransactionDTO::getCategory,
                            Collectors.summingDouble(TransactionDTO::getAmount)
                        ));

                    spendingHistory.put("totalSpent", totalSpent);
                    spendingHistory.put("byCategory", new HashMap<>(byCategory));
                }
            } catch (Exception e) {
                System.err.println("ERROR fetching spending history: " + e.getMessage());
            }

            // Prepare AI request
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("userId", userId);
            aiRequest.put("newCategory", newCategory);
            aiRequest.put("newAmount", newAmount);
            aiRequest.put("newPeriod", newPeriod);
            aiRequest.put("startDate", startDate);
            aiRequest.put("endDate", endDate);

            // Add existing budgets context
            aiRequest.put("existingBudgets", existingBudgets.stream()
                .map(this::mapToSuggestionContext)
                .collect(Collectors.toList()));

            // Add recurring incomes
            aiRequest.put("recurringIncomes", recurringIncomes);

            // Add spending history
            aiRequest.put("spendingHistory", spendingHistory);

            // Call AI Service
            String aiServiceUrl = "http://ai-service:8085/suggest-budget";
            ApiResponseWrapper<String> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl)
                .header("X-User-Id", userId.toString())
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponseWrapper<String>>() {})
                .block();

            return (response != null && response.getData() != null)
                ? response.getData()
                : "{\"shouldCreate\":true,\"recommendation\":\"Không thể kết nối AI service\",\"reason\":\"Dịch vụ tạm thời không khả dụng\",\"suggestedAmount\":null,\"warnings\":[],\"tips\":[]}";

        } catch (Exception e) {
            System.err.println("ERROR in checkBudgetBeforeCreate: " + e.getMessage());
            e.printStackTrace();
            return "{\"shouldCreate\":true,\"recommendation\":\"Đã xảy ra lỗi khi phân tích\",\"reason\":\"" + e.getMessage() + "\",\"suggestedAmount\":null,\"warnings\":[\"Không thể phân tích với AI\"],\"tips\":[\"Vui lòng kiểm tra thủ công\"]}";
        }
    }

    /**
     * Get AI-powered budget suggestion
     */
    public String getAISuggestion(Long userId, Map<String, Object> request) {
        try {
            // Get existing budgets for context
            List<Budget> existingBudgets = budgetRepository.findByUserIdAndIsActiveTrue(userId);

            // Prepare data for AI
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("userId", userId);
            aiRequest.put("existingBudgets", existingBudgets.stream()
                .map(this::mapToSuggestionContext)
                .collect(Collectors.toList()));

            // Add any additional context from request
            if (request != null) {
                aiRequest.putAll(request);
            }

            // Call AI Service
            String aiServiceUrl = "http://ai-service:8085/suggest-budget";
            ApiResponseWrapper<String> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl)
                .header("X-User-Id", userId.toString())
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponseWrapper<String>>() {})
                .block();

            return (response != null && response.getData() != null)
                ? response.getData()
                : "Không thể tạo gợi ý. Vui lòng thử lại sau.";

        } catch (Exception e) {
            System.err.println("ERROR calling AI service for budget suggestion: " + e.getMessage());
            e.printStackTrace();
            return "Dịch vụ AI tạm thời không khả dụng. Vui lòng kiểm tra:\n" +
                   "1. Các ngân sách hiện tại của bạn\n" +
                   "2. Lịch sử chi tiêu trong các danh mục\n" +
                   "3. Cân đối giữa thu nhập và chi tiêu\n" +
                   "4. Áp dụng quy tắc 50/30/20 cho ngân sách";
        }
    }

    /**
     * Map budget to simplified context for AI suggestion
     */
    private Map<String, Object> mapToSuggestionContext(Budget budget) {
        Map<String, Object> context = new HashMap<>();
        context.put("category", budget.getCategory());
        context.put("amount", budget.getAmount());
        context.put("spentAmount", calculateSpentAmount(budget));
        context.put("period", budget.getPeriod().toString());
        context.put("startDate", budget.getStartDate().toString());
        context.put("endDate", budget.getEndDate().toString());
        return context;
    }

    /**
     * Calculate spent amount from transactions for this budget
     */
    private double calculateSpentAmount(Budget budget) {
        try {
            // Call Transaction Service to get transactions for this user, category, and date range
            String transactionServiceUrl = String.format(
                "http://transaction-service:8082/date-range?startDate=%s&endDate=%s",
                budget.getStartDate(),
                budget.getEndDate()
            );

            System.out.println("=== BUDGET SERVICE DEBUG ===");
            System.out.println("Calling Transaction Service URL: " + transactionServiceUrl);
            System.out.println("User ID: " + budget.getUserId());
            System.out.println("Budget Category: " + budget.getCategory());
            System.out.println("Date Range: " + budget.getStartDate() + " to " + budget.getEndDate());

            // Call Transaction Service with user ID in header
            // The response is wrapped in ApiResponse<List<TransactionDTO>>
            ApiResponseWrapper<java.util.List<TransactionDTO>> response = webClientBuilder.build()
                .get()
                .uri(transactionServiceUrl)
                .header("X-User-Id", budget.getUserId().toString())
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponseWrapper<java.util.List<TransactionDTO>>>() {})
                .block();

            System.out.println("Response received: " + response);
            List<TransactionDTO> transactions = (response != null && response.getData() != null)
                ? response.getData()
                : new java.util.ArrayList<>();

            System.out.println("Transactions received: " + transactions.size());
            if (!transactions.isEmpty()) {
                transactions.forEach(t ->
                    System.out.println("  - Type: " + t.getType() + ", Category: " + t.getCategory() + ", Amount: " + t.getAmount())
                );
            }

            if (transactions.isEmpty()) {
                System.out.println("No transactions found, returning 0.0");
                return 0.0;
            }

            // Sum expenses for this category
            double spent = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .filter(t -> budget.getCategory().equals(t.getCategory()))
                .mapToDouble(TransactionDTO::getAmount)
                .sum();

            System.out.println("Total spent calculated: " + spent);
            System.out.println("=== END DEBUG ===");

            return spent;

        } catch (Exception e) {
            // If transaction service is unavailable, return stored spent amount
            System.err.println("ERROR calling transaction service: " + e.getMessage());
            e.printStackTrace();
            return budget.getSpentAmount() != null ? budget.getSpentAmount() : 0.0;
        }
    }

    // DTO for API Response wrapper
    @lombok.Data
    private static class ApiResponseWrapper<T> {
        private boolean success;
        private String message;
        private T data;
        private java.time.LocalDateTime timestamp;
    }

    // DTO for transaction data from Transaction Service
    @lombok.Data
    private static class TransactionDTO {
        private String type;
        private String category;
        private Double amount;
    }
}
