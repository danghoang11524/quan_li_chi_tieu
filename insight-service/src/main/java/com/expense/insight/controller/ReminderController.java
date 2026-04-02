package com.expense.insight.controller;

import com.expense.insight.dto.ReminderRequest;
import com.expense.insight.entity.Reminder;
import com.expense.insight.entity.Reminder.ReminderType;
import com.expense.insight.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping("/daily-transaction")
    public ResponseEntity<Reminder> createDailyTransactionReminder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "21:00") String time) {
        LocalTime reminderTime = LocalTime.parse(time);
        return ResponseEntity.ok(reminderService.createDailyTransactionReminder(userId, reminderTime));
    }

    @PostMapping("/bill-payment")
    public ResponseEntity<Reminder> createBillPaymentReminder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ReminderRequest request) {
        Reminder reminder = reminderService.createBillPaymentReminder(
                userId,
                request.getTitle(),
                request.getDueDate(),
                request.getReminderTime() != null ? request.getReminderTime() : LocalTime.of(10, 0)
        );
        return ResponseEntity.ok(reminder);
    }

    @PostMapping("/custom")
    public ResponseEntity<Reminder> createCustomReminder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ReminderRequest request) {
        Reminder reminder = reminderService.createCustomReminder(
                userId,
                request.getTitle(),
                request.getDescription(),
                request.getDueDate(),
                request.getReminderTime(),
                request.getFrequency()
        );
        return ResponseEntity.ok(reminder);
    }

    @GetMapping
    public ResponseEntity<List<Reminder>> getUserReminders(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(reminderService.getUserReminders(userId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<Reminder>> getActiveReminders(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(reminderService.getActiveReminders(userId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Reminder>> getRemindersByType(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable ReminderType type) {
        return ResponseEntity.ok(reminderService.getRemindersByType(userId, type));
    }

    @GetMapping("/{reminderId}/message")
    public ResponseEntity<String> getReminderMessage(@PathVariable Long reminderId) {
        return ResponseEntity.ok(reminderService.getReminderMessage(reminderId));
    }

    @PutMapping("/{reminderId}")
    public ResponseEntity<Reminder> updateReminder(
            @PathVariable Long reminderId,
            @RequestBody ReminderRequest request) {
        Reminder reminder = reminderService.updateReminder(
                reminderId,
                request.getTitle(),
                request.getDescription(),
                request.getDueDate(),
                request.getReminderTime(),
                request.getFrequency(),
                request.getActive()
        );
        return ResponseEntity.ok(reminder);
    }

    @PutMapping("/{reminderId}/toggle")
    public ResponseEntity<Reminder> toggleReminder(@PathVariable Long reminderId) {
        return ResponseEntity.ok(reminderService.toggleReminder(reminderId));
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long reminderId) {
        reminderService.deleteReminder(reminderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/initialize-defaults")
    public ResponseEntity<List<Reminder>> initializeDefaultReminders(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(reminderService.createDefaultReminders(userId));
    }
}
