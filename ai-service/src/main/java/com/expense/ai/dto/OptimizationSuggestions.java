package com.expense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho gợi ý tối ưu chi tiêu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationSuggestions {

    /**
     * Tổng số tiền có thể tiết kiệm
     */
    private double totalSavingsPotential;

    /**
     * Phần trăm tiết kiệm so với chi tiêu hiện tại
     */
    private double savingsPercentage;

    /**
     * Tóm tắt tổng quan
     */
    private String summary;

    /**
     * Danh sách gợi ý tối ưu theo từng danh mục
     */
    private List<CategoryOptimization> categoryOptimizations;

    /**
     * Gợi ý hành động ưu tiên cao
     */
    private List<String> priorityActions;

    /**
     * Gợi ý dài hạn
     */
    private List<String> longTermSuggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryOptimization {
        /**
         * Danh mục chi tiêu
         */
        private String category;

        /**
         * Chi tiêu hiện tại
         */
        private double currentSpending;

        /**
         * Chi tiêu tối ưu được gợi ý
         */
        private double optimizedSpending;

        /**
         * Số tiền tiết kiệm được
         */
        private double savingsPotential;

        /**
         * Phần trăm giảm
         */
        private double reductionPercentage;

        /**
         * Mức độ ưu tiên (HIGH, MEDIUM, LOW)
         */
        private String priority;

        /**
         * Độ khó thực hiện (EASY, MODERATE, HARD)
         */
        private String difficulty;

        /**
         * Mô tả cách tối ưu
         */
        private String description;

        /**
         * Các bước hành động cụ thể
         */
        private List<String> actionSteps;

        /**
         * Lợi ích khi thực hiện
         */
        private String benefits;
    }
}
