package com.expense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho phân tích bất thường chi tiêu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyAnalysis {

    /**
     * Có phát hiện bất thường không
     */
    private boolean hasAnomalies;

    /**
     * Tổng số bất thường phát hiện
     */
    private int anomalyCount;

    /**
     * Mức độ bất thường tổng thể (LOW, MEDIUM, HIGH, CRITICAL)
     */
    private String overallSeverity;

    /**
     * Danh sách các bất thường phát hiện được
     */
    private List<SpendingAnomaly> anomalies;

    /**
     * Tóm tắt phân tích
     */
    private String summary;

    /**
     * Gợi ý hành động
     */
    private List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpendingAnomaly {
        /**
         * Danh mục chi tiêu
         */
        private String category;

        /**
         * Chi tiêu tháng hiện tại
         */
        private double currentAmount;

        /**
         * Chi tiêu trung bình tháng trước
         */
        private double previousAmount;

        /**
         * Phần trăm thay đổi
         */
        private double changePercentage;

        /**
         * Mức độ bất thường (LOW, MEDIUM, HIGH, CRITICAL)
         */
        private String severity;

        /**
         * Mô tả bất thường
         */
        private String description;

        /**
         * Nguyên nhân có thể
         */
        private String possibleCause;

        /**
         * Gợi ý xử lý
         */
        private String recommendation;
    }
}
