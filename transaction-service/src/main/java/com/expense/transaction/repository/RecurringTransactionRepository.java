package com.expense.transaction.repository;

import com.expense.transaction.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RecurringTransaction> findByUserIdAndIsActiveOrderByCreatedAtDesc(Long userId, Boolean isActive);

    @Query("SELECT r FROM RecurringTransaction r WHERE r.isActive = true " +
           "AND r.startDate <= :currentDate " +
           "AND (r.endDate IS NULL OR r.endDate >= :currentDate)")
    List<RecurringTransaction> findActiveRecurringTransactionsDueForGeneration(LocalDate currentDate);
}
