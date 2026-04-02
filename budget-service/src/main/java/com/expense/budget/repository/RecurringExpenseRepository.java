package com.expense.budget.repository;

import com.expense.budget.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<RecurringExpense> findByIdAndUserId(Long id, Long userId);

    List<RecurringExpense> findByUserIdAndIsActiveOrderByCreatedAtDesc(Long userId, Boolean isActive);

    @Query("SELECT r FROM RecurringExpense r WHERE r.isActive = true AND " +
           "(r.lastGeneratedDate IS NULL OR r.lastGeneratedDate < :currentDate) AND " +
           "r.startDate <= :currentDate AND " +
           "(r.endDate IS NULL OR r.endDate >= :currentDate)")
    List<RecurringExpense> findActiveDueForGeneration(LocalDate currentDate);

    void deleteByIdAndUserId(Long id, Long userId);
}
