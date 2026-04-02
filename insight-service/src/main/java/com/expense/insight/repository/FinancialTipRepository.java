package com.expense.insight.repository;

import com.expense.insight.entity.FinancialTip;
import com.expense.insight.entity.FinancialTip.TipCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialTipRepository extends JpaRepository<FinancialTip, Long> {

    List<FinancialTip> findByActiveOrderByPriorityDesc(Boolean active);

    List<FinancialTip> findByCategoryAndActiveOrderByPriorityDesc(TipCategory category, Boolean active);

    @Query("SELECT t FROM FinancialTip t WHERE t.active = true ORDER BY RAND() LIMIT 1")
    FinancialTip findRandomTip();

    @Query("SELECT t FROM FinancialTip t WHERE t.active = true ORDER BY RAND()")
    List<FinancialTip> findRandomTips();
}
