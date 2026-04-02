package com.expense.budget.repository;

import com.expense.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndIsActiveTrue(Long userId);
    List<Budget> findByUserIdAndCategory(Long userId, String category);
    List<Budget> findByUserId(Long userId);
}
