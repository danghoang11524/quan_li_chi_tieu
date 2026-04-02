package com.expense.insight.service;

import com.expense.insight.entity.SavingChallenge;
import com.expense.insight.entity.SavingChallenge.ChallengeStatus;
import com.expense.insight.entity.SavingChallenge.ChallengeType;
import com.expense.insight.repository.SavingChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavingChallengeService {

    private final SavingChallengeRepository challengeRepository;

    /**
     * Tạo thử thách tiết kiệm 7 ngày
     * Mỗi ngày tiết kiệm một số tiền nhất định
     */
    public SavingChallenge create7DayChallenge(Long userId, BigDecimal dailyAmount) {
        // Kiểm tra xem đã có challenge đang active không
        Optional<SavingChallenge> existing = challengeRepository
            .findByUserIdAndTypeAndStatus(userId, ChallengeType.SEVEN_DAY, ChallengeStatus.ACTIVE);

        if (existing.isPresent()) {
            throw new RuntimeException("Bạn đã có thử thách 7 ngày đang hoạt động!");
        }

        SavingChallenge challenge = new SavingChallenge();
        challenge.setUserId(userId);
        challenge.setType(ChallengeType.SEVEN_DAY);
        challenge.setStartDate(LocalDate.now());
        challenge.setEndDate(LocalDate.now().plusDays(7));
        challenge.setCurrentDay(1);
        challenge.setCurrentWeek(1);
        challenge.setTotalSaved(BigDecimal.ZERO);
        challenge.setTargetAmount(dailyAmount.multiply(BigDecimal.valueOf(7)));
        challenge.setStatus(ChallengeStatus.ACTIVE);

        return challengeRepository.save(challenge);
    }

    /**
     * Tạo thử thách tiết kiệm 52 tuần
     * Tuần 1: 10,000đ, Tuần 2: 20,000đ, ..., Tuần 52: 520,000đ
     * Tổng: 13,780,000đ
     */
    public SavingChallenge create52WeekChallenge(Long userId, boolean reverse) {
        Optional<SavingChallenge> existing = challengeRepository
            .findByUserIdAndTypeAndStatus(userId, ChallengeType.FIFTY_TWO_WEEK, ChallengeStatus.ACTIVE);

        if (existing.isPresent()) {
            throw new RuntimeException("Bạn đã có thử thách 52 tuần đang hoạt động!");
        }

        SavingChallenge challenge = new SavingChallenge();
        challenge.setUserId(userId);
        challenge.setType(ChallengeType.FIFTY_TWO_WEEK);
        challenge.setStartDate(LocalDate.now());
        challenge.setEndDate(LocalDate.now().plusWeeks(52));
        challenge.setCurrentWeek(1);
        challenge.setCurrentDay(1);
        challenge.setTotalSaved(BigDecimal.ZERO);
        challenge.setTargetAmount(BigDecimal.valueOf(13_780_000)); // Tổng 52 tuần
        challenge.setStatus(ChallengeStatus.ACTIVE);

        return challengeRepository.save(challenge);
    }

    /**
     * Ghi nhận tiết kiệm cho ngày/tuần hiện tại
     */
    public SavingChallenge recordSaving(Long challengeId, BigDecimal amount) {
        SavingChallenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new RuntimeException("Challenge không tồn tại!"));

        if (challenge.getStatus() != ChallengeStatus.ACTIVE) {
            throw new RuntimeException("Challenge này không còn active!");
        }

        // Cập nhật tổng tiết kiệm
        challenge.setTotalSaved(challenge.getTotalSaved().add(amount));

        // Cập nhật tiến độ
        if (challenge.getType() == ChallengeType.SEVEN_DAY) {
            challenge.setCurrentDay(challenge.getCurrentDay() + 1);
            if (challenge.getCurrentDay() > 7) {
                challenge.setStatus(ChallengeStatus.COMPLETED);
            }
        } else if (challenge.getType() == ChallengeType.FIFTY_TWO_WEEK) {
            challenge.setCurrentWeek(challenge.getCurrentWeek() + 1);
            if (challenge.getCurrentWeek() > 52) {
                challenge.setStatus(ChallengeStatus.COMPLETED);
            }
        }

        return challengeRepository.save(challenge);
    }

    /**
     * Lấy tất cả challenges của user
     */
    public List<SavingChallenge> getUserChallenges(Long userId) {
        return challengeRepository.findByUserId(userId);
    }

    /**
     * Lấy challenges đang active
     */
    public List<SavingChallenge> getActiveChallenges(Long userId) {
        return challengeRepository.findByUserIdAndStatus(userId, ChallengeStatus.ACTIVE);
    }

    /**
     * Lấy thông tin chi tiết về số tiền cần tiết kiệm
     */
    public Map<String, Object> getChallengeDetails(Long challengeId) {
        SavingChallenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new RuntimeException("Challenge không tồn tại!"));

        Map<String, Object> details = new HashMap<>();
        details.put("challenge", challenge);

        if (challenge.getType() == ChallengeType.SEVEN_DAY) {
            details.put("description", "Thử thách 7 ngày: Tiết kiệm mỗi ngày trong 7 ngày liên tục");
            details.put("remainingDays", 7 - challenge.getCurrentDay() + 1);
            details.put("completionRate", (challenge.getCurrentDay() - 1) * 100.0 / 7);
        } else if (challenge.getType() == ChallengeType.FIFTY_TWO_WEEK) {
            int week = challenge.getCurrentWeek();
            BigDecimal weeklyAmount = BigDecimal.valueOf(week * 10_000);

            details.put("description", "Thử thách 52 tuần: Mỗi tuần tiết kiệm tăng dần");
            details.put("currentWeek", week);
            details.put("weeklyAmount", weeklyAmount);
            details.put("remainingWeeks", 52 - week + 1);
            details.put("completionRate", (week - 1) * 100.0 / 52);

            // Tính lịch tiết kiệm cho 4 tuần tiếp theo
            Map<Integer, BigDecimal> upcomingWeeks = new HashMap<>();
            for (int i = 0; i < 4 && (week + i) <= 52; i++) {
                upcomingWeeks.put(week + i, BigDecimal.valueOf((week + i) * 10_000));
            }
            details.put("upcomingWeeks", upcomingWeeks);
        }

        // Tính phần trăm hoàn thành dựa trên số tiền
        if (challenge.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            double moneyCompletionRate = challenge.getTotalSaved()
                .divide(challenge.getTargetAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
            details.put("moneyCompletionRate", moneyCompletionRate);
        }

        return details;
    }

    /**
     * Tạm dừng challenge
     */
    public SavingChallenge pauseChallenge(Long challengeId) {
        SavingChallenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new RuntimeException("Challenge không tồn tại!"));

        challenge.setStatus(ChallengeStatus.PAUSED);
        return challengeRepository.save(challenge);
    }

    /**
     * Tiếp tục challenge đã tạm dừng
     */
    public SavingChallenge resumeChallenge(Long challengeId) {
        SavingChallenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new RuntimeException("Challenge không tồn tại!"));

        if (challenge.getStatus() != ChallengeStatus.PAUSED) {
            throw new RuntimeException("Chỉ có thể resume challenge đang paused!");
        }

        challenge.setStatus(ChallengeStatus.ACTIVE);
        return challengeRepository.save(challenge);
    }

    /**
     * Hủy challenge
     */
    public SavingChallenge cancelChallenge(Long challengeId) {
        SavingChallenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new RuntimeException("Challenge không tồn tại!"));

        challenge.setStatus(ChallengeStatus.FAILED);
        return challengeRepository.save(challenge);
    }

    /**
     * Lấy động lực (motivation) dựa trên tiến độ
     */
    public String getMotivationMessage(Long challengeId) {
        Map<String, Object> details = getChallengeDetails(challengeId);
        SavingChallenge challenge = (SavingChallenge) details.get("challenge");
        double completionRate = (double) details.getOrDefault("completionRate", 0.0);

        StringBuilder message = new StringBuilder();

        if (challenge.getType() == ChallengeType.SEVEN_DAY) {
            message.append("🎯 Thử thách 7 ngày\n\n");
            message.append(String.format("Bạn đang ở ngày %d/7\n", challenge.getCurrentDay()));
            message.append(String.format("Đã tiết kiệm: %,.0f VNĐ\n", challenge.getTotalSaved()));
        } else {
            message.append("🎯 Thử thách 52 tuần\n\n");
            message.append(String.format("Bạn đang ở tuần %d/52\n", challenge.getCurrentWeek()));
            message.append(String.format("Đã tiết kiệm: %,.0f VNĐ / %,.0f VNĐ\n",
                challenge.getTotalSaved(), challenge.getTargetAmount()));
        }

        message.append("\n");

        if (completionRate < 25) {
            message.append("💪 Hành trình vạn dặm bắt đầu từ bước chân đầu tiên! Tiếp tục phát huy nhé!");
        } else if (completionRate < 50) {
            message.append("🔥 Tuyệt vời! Bạn đã hoàn thành 1/4 hành trình. Động lực lên nào!");
        } else if (completionRate < 75) {
            message.append("⭐ Quá nửa chặng đường rồi! Bạn đang làm rất tốt, đừng bỏ cuộc!");
        } else if (completionRate < 100) {
            message.append("🏆 Sắp đến đích rồi! Chỉ còn một chút nữa thôi, cố lên!");
        } else {
            message.append("🎉 HOÀN THÀNH! Xin chúc mừng, bạn đã chứng minh được sự kiên trì của mình!");
        }

        return message.toString();
    }

    /**
     * Lấy số tiền cần tiết kiệm cho tuần/ngày tiếp theo
     */
    public BigDecimal getNextSavingAmount(Long challengeId) {
        SavingChallenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new RuntimeException("Challenge không tồn tại!"));

        if (challenge.getType() == ChallengeType.FIFTY_TWO_WEEK) {
            return BigDecimal.valueOf(challenge.getCurrentWeek() * 10_000);
        } else {
            // 7-day challenge có số tiền cố định
            return challenge.getTargetAmount().divide(BigDecimal.valueOf(7), 0, BigDecimal.ROUND_UP);
        }
    }
}
