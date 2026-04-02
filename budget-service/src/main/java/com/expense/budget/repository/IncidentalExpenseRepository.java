package com.expense.budget.repository;

import com.expense.budget.entity.ExpenseCategory;
import com.expense.budget.entity.IncidentalExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentalExpenseRepository extends JpaRepository<IncidentalExpense, Long> {

    List<IncidentalExpense> findByUserIdOrderByExpenseDateDesc(Long userId);

    Optional<IncidentalExpense> findByIdAndUserId(Long id, Long userId);

    List<IncidentalExpense> findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<IncidentalExpense> findByUserIdAndCategoryOrderByExpenseDateDesc(
            Long userId, ExpenseCategory category);

    @Query("SELECT SUM(i.amount) FROM IncidentalExpense i WHERE i.userId = :userId")
    Double calculateTotalByUserId(Long userId);

    @Query("SELECT SUM(i.amount) FROM IncidentalExpense i WHERE i.userId = :userId " +
           "AND i.expenseDate BETWEEN :startDate AND :endDate")
    Double calculateTotalByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    Long countByUserId(Long userId);

    Long countByUserIdAndExpenseDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    void deleteByIdAndUserId(Long id, Long userId);
}
