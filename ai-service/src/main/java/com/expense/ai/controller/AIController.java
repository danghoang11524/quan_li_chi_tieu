package com.expense.ai.controller;

import com.expense.ai.dto.ChatRequest;
import com.expense.ai.dto.ChatResponse;
import com.expense.ai.dto.SpendingAnalysis;
import com.expense.ai.dto.SpendingPrediction;
import com.expense.ai.service.AIAnalysisService;
import com.expense.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AIController {

    private final AIAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<SpendingAnalysis>> analyzeSpending(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> transactionData) {
        SpendingAnalysis analysis = aiAnalysisService.analyzeSpending(userId, transactionData);
        return ResponseEntity.ok(ApiResponse.success("Analysis completed successfully", analysis));
    }

    @PostMapping("/predict")
    public ResponseEntity<ApiResponse<SpendingPrediction>> predictSpending(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> historicalData) {
        SpendingPrediction prediction = aiAnalysisService.predictSpending(userId, historicalData);
        return ResponseEntity.ok(ApiResponse.success("Prediction generated successfully", prediction));
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = aiAnalysisService.chat(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/statistics")
    public ResponseEntity<ApiResponse<com.expense.ai.dto.StatisticsResponse>> analyzeStatistics(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody com.expense.ai.dto.StatisticsRequest request) {
        com.expense.ai.dto.StatisticsResponse response = aiAnalysisService.analyzeStatistics(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Statistics analysis completed", response));
    }

    /**
     * API mới: Tự động nhận diện danh mục từ mô tả giao dịch
     */
    @PostMapping("/categorize")
    public ResponseEntity<ApiResponse<Map<String, String>>> categorizeTransaction(
            @RequestBody Map<String, String> request) {
        String description = request.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Description is required"));
        }

        String category = aiAnalysisService.categorizeTransaction(description);
        return ResponseEntity.ok(ApiResponse.success(
                "Transaction categorized successfully",
                Map.of("category", category, "description", description)
        ));
    }

    /**
     * API mới: Phân tích bất thường chi tiêu so với tháng trước
     */
    @PostMapping("/anomaly")
    public ResponseEntity<ApiResponse<com.expense.ai.dto.AnomalyAnalysis>> detectAnomalies(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {
        com.expense.ai.dto.AnomalyAnalysis analysis = aiAnalysisService.detectSpendingAnomalies(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Anomaly detection completed", analysis));
    }

    /**
     * API mới: Gợi ý tối ưu chi tiêu thông minh
     */
    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<com.expense.ai.dto.OptimizationSuggestions>> optimizeSpending(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {
        com.expense.ai.dto.OptimizationSuggestions suggestions = aiAnalysisService.generateOptimizationSuggestions(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Optimization suggestions generated", suggestions));
    }

    /**
     * API mới: Phân tích tính khả thi của mục tiêu tiết kiệm
     * Dựa trên thu nhập, chi tiêu cố định (recurring), và các mục tiêu hiện có
     */
    @PostMapping("/analyze-savings-goal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeSavingsGoalFeasibility(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> analysis = aiAnalysisService.analyzeSavingsGoalFeasibility(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Savings goal analysis completed", analysis));
    }

    @PostMapping("/suggest-budget")
    public ResponseEntity<ApiResponse<String>> suggestBudget(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request) {
        String suggestion = aiAnalysisService.suggestBudget(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Budget suggestion generated", suggestion));
    }
}
