package com.expense.insight.repository;

import com.expense.insight.entity.SavingChallenge;
import com.expense.insight.entity.SavingChallenge.ChallengeStatus;
import com.expense.insight.entity.SavingChallenge.ChallengeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingChallengeRepository extends JpaRepository<SavingChallenge, Long> {

    List<SavingChallenge> findByUserId(Long userId);

    List<SavingChallenge> findByUserIdAndStatus(Long userId, ChallengeStatus status);

    Optional<SavingChallenge> findByUserIdAndTypeAndStatus(Long userId, ChallengeType type, ChallengeStatus status);

    List<SavingChallenge> findByStatus(ChallengeStatus status);
}
