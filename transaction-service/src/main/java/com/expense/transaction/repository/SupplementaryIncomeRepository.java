package com.expense.transaction.repository;

import com.expense.transaction.entity.IncomeCategory;
import com.expense.transaction.entity.SupplementaryIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplementaryIncomeRepository extends JpaRepository<SupplementaryIncome, Long> {

    List<SupplementaryIncome> findByUserIdOrderByIncomeDateDesc(Long userId);

    Optional<SupplementaryIncome> findByIdAndUserId(Long id, Long userId);

    List<SupplementaryIncome> findByUserIdAndIncomeDateBetweenOrderByIncomeDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<SupplementaryIncome> findByUserIdAndCategoryOrderByIncomeDateDesc(
            Long userId, IncomeCategory category);

    @Query("SELECT SUM(s.amount) FROM SupplementaryIncome s WHERE s.userId = :userId")
    Double calculateTotalByUserId(Long userId);

    @Query("SELECT SUM(s.amount) FROM SupplementaryIncome s WHERE s.userId = :userId " +
           "AND s.incomeDate BETWEEN :startDate AND :endDate")
    Double calculateTotalByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    Long countByUserId(Long userId);

    Long countByUserIdAndIncomeDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    void deleteByIdAndUserId(Long id, Long userId);
}
