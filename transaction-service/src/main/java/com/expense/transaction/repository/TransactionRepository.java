package com.expense.transaction.repository;

import com.expense.transaction.entity.Transaction;
import com.expense.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndTypeOrderByTransactionDateDesc(Long userId, TransactionType type);

    List<Transaction> findByUserIdAndCategoryOrderByTransactionDateDesc(Long userId, String category);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.type = :type")
    Double sumByUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.type = :type " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double sumByUserIdAndTypeAndDateRange(@Param("userId") Long userId,
                                          @Param("type") TransactionType type,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}
