package com.expense.insight.service;

import com.expense.insight.entity.Reminder;
import com.expense.insight.entity.Reminder.ReminderFrequency;
import com.expense.insight.entity.Reminder.ReminderType;
import com.expense.insight.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;

    /**
     * Tạo nhắc nhở ghi chép giao dịch hàng ngày
     */
    public Reminder createDailyTransactionReminder(Long userId, LocalTime reminderTime) {
        Reminder reminder = new Reminder();
        reminder.setUserId(userId);
        reminder.setType(ReminderType.DAILY_TRANSACTION);
        reminder.setTitle("Nhắc ghi chép giao dịch");
        reminder.setDescription("Đừng quên ghi lại các giao dịch hôm nay để quản lý tài chính tốt hơn!");
        reminder.setReminderTime(reminderTime);
        reminder.setFrequency(ReminderFrequency.DAILY);
        reminder.setActive(true);

        return reminderRepository.save(reminder);
    }

    /**
     * Tạo nhắc nhở thanh toán hóa đơn
     */
    public Reminder createBillPaymentReminder(Long userId, String billName,
                                              LocalDate dueDate, LocalTime reminderTime) {
        Reminder reminder = new Reminder();
        reminder.setUserId(userId);
        reminder.setType(ReminderType.BILL_PAYMENT);
        reminder.setTitle("Nhắc thanh toán: " + billName);
        reminder.setDescription(String.format("Hóa đơn %s sắp đến hạn. Hãy thanh toán để tránh phí phạt!", billName));
        reminder.setDueDate(dueDate);
        reminder.setReminderTime(reminderTime);
        reminder.setFrequency(ReminderFrequency.MONTHLY);
        reminder.setActive(true);

        return reminderRepository.save(reminder);
    }

    /**
     * Tạo nhắc nhở tùy chỉnh
     */
    public Reminder createCustomReminder(Long userId, String title, String description,
                                        LocalDate dueDate, LocalTime reminderTime,
                                        ReminderFrequency frequency) {
        Reminder reminder = new Reminder();
        reminder.setUserId(userId);
        reminder.setType(ReminderType.CUSTOM);
        reminder.setTitle(title);
        reminder.setDescription(description);
        reminder.setDueDate(dueDate);
        reminder.setReminderTime(reminderTime);
        reminder.setFrequency(frequency);
        reminder.setActive(true);

        return reminderRepository.save(reminder);
    }

    /**
     * Lấy tất cả nhắc nhở của user
     */
    public List<Reminder> getUserReminders(Long userId) {
        return reminderRepository.findByUserId(userId);
    }

    /**
     * Lấy nhắc nhở đang active
     */
    public List<Reminder> getActiveReminders(Long userId) {
        return reminderRepository.findByUserIdAndActive(userId, true);
    }

    /**
     * Lấy nhắc nhở theo loại
     */
    public List<Reminder> getRemindersByType(Long userId, ReminderType type) {
        return reminderRepository.findByUserIdAndType(userId, type);
    }

    /**
     * Cập nhật nhắc nhở
     */
    public Reminder updateReminder(Long reminderId, String title, String description,
                                   LocalDate dueDate, LocalTime reminderTime,
                                   ReminderFrequency frequency, Boolean active) {
        Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new RuntimeException("Reminder không tồn tại!"));

        if (title != null) reminder.setTitle(title);
        if (description != null) reminder.setDescription(description);
        if (dueDate != null) reminder.setDueDate(dueDate);
        if (reminderTime != null) reminder.setReminderTime(reminderTime);
        if (frequency != null) reminder.setFrequency(frequency);
        if (active != null) reminder.setActive(active);

        return reminderRepository.save(reminder);
    }

    /**
     * Xóa nhắc nhở
     */
    public void deleteReminder(Long reminderId) {
        reminderRepository.deleteById(reminderId);
    }

    /**
     * Tắt/bật nhắc nhở
     */
    public Reminder toggleReminder(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new RuntimeException("Reminder không tồn tại!"));

        reminder.setActive(!reminder.getActive());
        return reminderRepository.save(reminder);
    }

    /**
     * Lấy danh sách nhắc nhở cần gửi hôm nay
     */
    public List<Reminder> getTodayReminders() {
        LocalDate today = LocalDate.now();
        return reminderRepository.findActiveDueReminders(today);
    }

    /**
     * Đánh dấu nhắc nhở đã được gửi
     */
    public Reminder markAsSent(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new RuntimeException("Reminder không tồn tại!"));

        reminder.setLastSent(LocalDateTime.now());

        // Tự động cập nhật ngày hẹn tiếp theo cho reminder định kỳ
        if (reminder.getFrequency() == ReminderFrequency.DAILY) {
            reminder.setDueDate(LocalDate.now().plusDays(1));
        } else if (reminder.getFrequency() == ReminderFrequency.WEEKLY) {
            reminder.setDueDate(reminder.getDueDate().plusWeeks(1));
        } else if (reminder.getFrequency() == ReminderFrequency.MONTHLY) {
            reminder.setDueDate(reminder.getDueDate().plusMonths(1));
        } else if (reminder.getFrequency() == ReminderFrequency.ONCE) {
            reminder.setActive(false); // Tự động tắt nếu chỉ nhắc 1 lần
        }

        return reminderRepository.save(reminder);
    }

    /**
     * Lấy nội dung nhắc nhở chi tiết
     */
    public String getReminderMessage(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new RuntimeException("Reminder không tồn tại!"));

        StringBuilder message = new StringBuilder();

        switch (reminder.getType()) {
            case DAILY_TRANSACTION:
                message.append("📝 Nhắc nhở ghi chép giao dịch\n\n");
                message.append("Hôm nay bạn đã ghi lại các giao dịch chưa?\n");
                message.append("Việc ghi chép đầy đủ sẽ giúp bạn:\n");
                message.append("✓ Biết rõ tiền đi đâu\n");
                message.append("✓ Phát hiện chi tiêu lãng phí\n");
                message.append("✓ Lập kế hoạch tài chính tốt hơn\n\n");
                message.append("Hãy dành 2 phút để cập nhật ngay!");
                break;

            case BILL_PAYMENT:
                message.append("💳 Nhắc nhở thanh toán hóa đơn\n\n");
                message.append(reminder.getTitle()).append("\n");
                if (reminder.getDueDate() != null) {
                    message.append("Hạn thanh toán: ").append(reminder.getDueDate()).append("\n");
                }
                if (reminder.getAmount() != null) {
                    message.append(String.format("Số tiền: %,.0f VNĐ\n", reminder.getAmount()));
                }
                message.append("\n").append(reminder.getDescription());
                break;

            case CUSTOM:
                message.append("🔔 ").append(reminder.getTitle()).append("\n\n");
                message.append(reminder.getDescription());
                break;
        }

        return message.toString();
    }

    /**
     * Tạo các nhắc nhở mẫu cho người dùng mới
     */
    public List<Reminder> createDefaultReminders(Long userId) {
        List<Reminder> reminders = new ArrayList<>();

        // Nhắc ghi chép giao dịch lúc 9 giờ tối
        Reminder dailyReminder = createDailyTransactionReminder(userId, LocalTime.of(21, 0));
        reminders.add(dailyReminder);

        // Nhắc thanh toán tiền điện (ngày 15 hàng tháng)
        Reminder electricBill = createBillPaymentReminder(
            userId,
            "Tiền điện",
            LocalDate.now().withDayOfMonth(15),
            LocalTime.of(10, 0)
        );
        reminders.add(electricBill);

        // Nhắc thanh toán tiền nước (ngày 20 hàng tháng)
        Reminder waterBill = createBillPaymentReminder(
            userId,
            "Tiền nước",
            LocalDate.now().withDayOfMonth(20),
            LocalTime.of(10, 0)
        );
        reminders.add(waterBill);

        // Nhắc thanh toán tiền internet (ngày 25 hàng tháng)
        Reminder internetBill = createBillPaymentReminder(
            userId,
            "Tiền internet",
            LocalDate.now().withDayOfMonth(25),
            LocalTime.of(10, 0)
        );
        reminders.add(internetBill);

        log.info("Created {} default reminders for user {}", reminders.size(), userId);
        return reminders;
    }

    /**
     * Scheduled job chạy mỗi giờ để kiểm tra reminders cần gửi
     * Trong production, nên tích hợp với notification service
     */
    @Scheduled(cron = "0 0 * * * *") // Chạy mỗi giờ
    public void checkAndSendReminders() {
        log.info("Checking for reminders to send...");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Reminder> dueReminders = reminderRepository.findActiveDueReminders(today);

        for (Reminder reminder : dueReminders) {
            // Kiểm tra xem đã đến giờ nhắc chưa
            if (reminder.getReminderTime() != null &&
                reminder.getReminderTime().isBefore(now.plusHours(1)) &&
                (reminder.getLastSent() == null ||
                 reminder.getLastSent().toLocalDate().isBefore(today))) {

                log.info("Sending reminder: {} to user: {}", reminder.getTitle(), reminder.getUserId());

                // TODO: Tích hợp với notification service để gửi thông báo
                // Tạm thời chỉ log
                log.info(getReminderMessage(reminder.getId()));

                // Đánh dấu đã gửi
                markAsSent(reminder.getId());
            }
        }
    }
}
