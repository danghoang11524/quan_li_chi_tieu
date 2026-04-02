package com.expense.insight.controller;

import com.expense.insight.dto.ChatRequest;
import com.expense.insight.dto.ChatResponse;
import com.expense.insight.service.AICoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-coach")
@RequiredArgsConstructor
@Slf4j
public class AICoachController {

    private final AICoachService aiCoachService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ChatRequest request) {
        log.info("AI Coach chat request from user: {}, message: {}", userId, request.getMessage());

        try {
            String response = aiCoachService.processChat(userId, request.getMessage());
            return ResponseEntity.ok(new ChatResponse(response));
        } catch (Exception e) {
            log.error("Error processing chat for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.ok(new ChatResponse(
                "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau."
            ));
        }
    }
}
