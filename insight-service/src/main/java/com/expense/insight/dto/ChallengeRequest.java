package com.expense.insight.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChallengeRequest {
    private String type; // "7_DAY" or "52_WEEK"
    private BigDecimal dailyAmount; // For 7-day challenge
    private Boolean reverse; // For 52-week challenge
}
