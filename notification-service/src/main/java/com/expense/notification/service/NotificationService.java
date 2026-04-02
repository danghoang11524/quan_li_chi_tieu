package com.expense.notification.service;

import com.expense.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    public void sendNotification(NotificationRequest request) {
        String type = request.getType() != null ? request.getType() : "EMAIL";

        switch (type) {
            case "EMAIL":
                sendEmail(request);
                break;
            case "SMS":
                sendSMS(request);
                break;
            case "PUSH":
                sendPushNotification(request);
                break;
            default:
                log.warn("Unknown notification type: {}", type);
        }
    }

    private void sendEmail(NotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getRecipientEmail());
            message.setSubject(request.getSubject());
            message.setText(request.getMessage());
            message.setFrom("noreply@expensemanagement.com");

            mailSender.send(message);
            log.info("Email sent successfully to: {}", request.getRecipientEmail());
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
    }

    private void sendSMS(NotificationRequest request) {
        // Integration with SMS service (Twilio, AWS SNS, etc.)
        log.info("SMS notification would be sent to: {}", request.getRecipientEmail());
    }

    private void sendPushNotification(NotificationRequest request) {
        // Integration with push notification service (Firebase, OneSignal, etc.)
        log.info("Push notification would be sent to: {}", request.getRecipientEmail());
    }

    public void sendBudgetAlert(String email, String category, Double spent, Double limit) {
        NotificationRequest request = NotificationRequest.builder()
                .recipientEmail(email)
                .subject("Budget Alert: " + category)
                .message(String.format(
                    "Warning! You've spent %.2f out of %.2f in the %s category. " +
                    "You're approaching your budget limit.",
                    spent, limit, category))
                .type("EMAIL")
                .build();

        sendNotification(request);
    }

    public void sendSavingsGoalReminder(String email, String goalName, Double required) {
        NotificationRequest request = NotificationRequest.builder()
                .recipientEmail(email)
                .subject("Savings Goal Reminder: " + goalName)
                .message(String.format(
                    "Don't forget to contribute to your '%s' goal! " +
                    "You need to save approximately %.2f today to stay on track.",
                    goalName, required))
                .type("EMAIL")
                .build();

        sendNotification(request);
    }
}
