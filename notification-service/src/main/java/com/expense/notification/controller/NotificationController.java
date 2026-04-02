package com.expense.notification.controller;

import com.expense.common.dto.ApiResponse;
import com.expense.notification.dto.NotificationRequest;
import com.expense.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody NotificationRequest request) {
        notificationService.sendNotification(request);
        return ResponseEntity.ok(ApiResponse.success("Notification sent successfully", null));
    }
}
