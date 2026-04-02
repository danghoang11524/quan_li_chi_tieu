package com.expense.insight.repository;

import com.expense.insight.entity.Reminder;
import com.expense.insight.entity.Reminder.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserId(Long userId);

    List<Reminder> findByUserIdAndActive(Long userId, Boolean active);

    List<Reminder> findByUserIdAndType(Long userId, ReminderType type);

    @Query("SELECT r FROM Reminder r WHERE r.active = true AND r.dueDate = :date")
    List<Reminder> findActiveDueReminders(LocalDate date);

    @Query("SELECT r FROM Reminder r WHERE r.active = true AND r.type = :type")
    List<Reminder> findActiveByType(ReminderType type);
}
