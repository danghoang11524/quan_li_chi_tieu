package com.expense.transaction.repository;

import com.expense.transaction.entity.FixedIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FixedIncomeRepository extends JpaRepository<FixedIncome, Long> {

    List<FixedIncome> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FixedIncome> findByIdAndUserId(Long id, Long userId);

    List<FixedIncome> findByUserIdAndIsActiveOrderByCreatedAtDesc(Long userId, Boolean isActive);

    @Query("SELECT f FROM FixedIncome f WHERE f.isActive = true AND " +
           "(f.lastGeneratedDate IS NULL OR f.lastGeneratedDate < :currentDate) AND " +
           "f.startDate <= :currentDate AND " +
           "(f.endDate IS NULL OR f.endDate >= :currentDate)")
    List<FixedIncome> findActiveDueForGeneration(LocalDate currentDate);

    void deleteByIdAndUserId(Long id, Long userId);
}
