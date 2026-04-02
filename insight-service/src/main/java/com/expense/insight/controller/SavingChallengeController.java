package com.expense.insight.controller;

import com.expense.insight.dto.ChallengeRequest;
import com.expense.insight.dto.SavingRecordRequest;
import com.expense.insight.entity.SavingChallenge;
import com.expense.insight.service.SavingChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
public class SavingChallengeController {

    private final SavingChallengeService challengeService;

    @PostMapping
    public ResponseEntity<SavingChallenge> createChallenge(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ChallengeRequest request) {

        SavingChallenge challenge;
        if ("7_DAY".equalsIgnoreCase(request.getType())) {
            BigDecimal dailyAmount = request.getDailyAmount() != null ?
                    request.getDailyAmount() : BigDecimal.valueOf(50000);
            challenge = challengeService.create7DayChallenge(userId, dailyAmount);
        } else if ("52_WEEK".equalsIgnoreCase(request.getType())) {
            boolean reverse = request.getReverse() != null ? request.getReverse() : false;
            challenge = challengeService.create52WeekChallenge(userId, reverse);
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(challenge);
    }

    @GetMapping
    public ResponseEntity<List<SavingChallenge>> getUserChallenges(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(challengeService.getUserChallenges(userId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<SavingChallenge>> getActiveChallenges(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(challengeService.getActiveChallenges(userId));
    }

    @GetMapping("/{challengeId}")
    public ResponseEntity<Map<String, Object>> getChallengeDetails(
            @PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.getChallengeDetails(challengeId));
    }

    @PostMapping("/{challengeId}/record")
    public ResponseEntity<SavingChallenge> recordSaving(
            @PathVariable Long challengeId,
            @RequestBody SavingRecordRequest request) {
        SavingChallenge challenge = challengeService.recordSaving(challengeId, request.getAmount());
        return ResponseEntity.ok(challenge);
    }

    @GetMapping("/{challengeId}/motivation")
    public ResponseEntity<String> getMotivation(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.getMotivationMessage(challengeId));
    }

    @GetMapping("/{challengeId}/next-amount")
    public ResponseEntity<BigDecimal> getNextAmount(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.getNextSavingAmount(challengeId));
    }

    @PutMapping("/{challengeId}/pause")
    public ResponseEntity<SavingChallenge> pauseChallenge(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.pauseChallenge(challengeId));
    }

    @PutMapping("/{challengeId}/resume")
    public ResponseEntity<SavingChallenge> resumeChallenge(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.resumeChallenge(challengeId));
    }

    @DeleteMapping("/{challengeId}")
    public ResponseEntity<SavingChallenge> cancelChallenge(@PathVariable Long challengeId) {
        return ResponseEntity.ok(challengeService.cancelChallenge(challengeId));
    }
}
