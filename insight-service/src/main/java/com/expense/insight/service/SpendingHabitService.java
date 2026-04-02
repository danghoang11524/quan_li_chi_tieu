package com.expense.insight.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpendingHabitService {

    private final WeatherService weatherService;

    public List<String> getSpendingHabitSuggestions(Long userId, String location) {
        List<String> suggestions = new ArrayList<>();

        // Lấy gợi ý dựa trên thời tiết
        if (location != null && !location.isEmpty()) {
            String weatherAdvice = weatherService.getWeatherBasedSpendingAdvice(location);
            if (weatherAdvice != null) {
                suggestions.add(weatherAdvice);
            }
        }

        // Gợi ý dựa trên ngày trong tuần
        suggestions.add(getDayOfWeekSuggestion());

        // Gợi ý dựa trên thời điểm trong tháng
        suggestions.add(getMonthPeriodSuggestion());

        // Gợi ý tổng quát
        suggestions.addAll(getGeneralSuggestions());

        return suggestions;
    }

    private String getDayOfWeekSuggestion() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        StringBuilder suggestion = new StringBuilder("📅 Gợi ý cho ");

        switch (today) {
            case MONDAY:
                suggestion.append("Thứ Hai:\n");
                suggestion.append("• Lập kế hoạch chi tiêu cho tuần mới\n");
                suggestion.append("• Chuẩn bị đồ ăn trưa từ nhà để tiết kiệm\n");
                suggestion.append("• Review chi tiêu tuần trước và điều chỉnh");
                break;
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
                suggestion.append("giữa tuần:\n");
                suggestion.append("• Kiểm tra ngân sách đã chi tiêu\n");
                suggestion.append("• Tránh mua sắm online không cần thiết\n");
                suggestion.append("• Tận dụng deals giữa tuần tại siêu thị");
                break;
            case FRIDAY:
                suggestion.append("Thứ Sáu:\n");
                suggestion.append("• ⚠️ Ngày dễ chi tiêu nhiều nhất! Hãy cẩn thận\n");
                suggestion.append("• Tránh ăn ngoài quá nhiều cuối tuần\n");
                suggestion.append("• Lập kế hoạch giải trí tiết kiệm cho cuối tuần");
                break;
            case SATURDAY:
            case SUNDAY:
                suggestion.append("cuối tuần:\n");
                suggestion.append("• Tận dụng hoạt động miễn phí (công viên, bảo tàng...)\n");
                suggestion.append("• Nấu ăn tại nhà thay vì ra ngoài\n");
                suggestion.append("• Tổng kết chi tiêu tuần và chuẩn bị cho tuần mới");
                break;
        }

        return suggestion.toString();
    }

    private String getMonthPeriodSuggestion() {
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        StringBuilder suggestion = new StringBuilder("📊 Gợi ý theo thời điểm trong tháng:\n");

        if (dayOfMonth <= 7) {
            suggestion.append("• Đầu tháng - thời điểm tốt để:\n");
            suggestion.append("  - Lập ngân sách cho cả tháng\n");
            suggestion.append("  - Thanh toán các hóa đơn định kỳ\n");
            suggestion.append("  - Chuyển tiền tiết kiệm trước khi chi tiêu");
        } else if (dayOfMonth <= 15) {
            suggestion.append("• Giữa tháng - lưu ý:\n");
            suggestion.append("  - Kiểm tra đã chi tiêu bao nhiêu % ngân sách\n");
            suggestion.append("  - Điều chỉnh chi tiêu nếu vượt quá 50%\n");
            suggestion.append("  - Tránh mua sắm xa xỉ");
        } else if (dayOfMonth <= 25) {
            suggestion.append("• Cuối tháng - cảnh báo:\n");
            suggestion.append("  - ⚠️ Thắt chặt chi tiêu nếu sắp hết ngân sách\n");
            suggestion.append("  - Ưu tiên các khoản chi cần thiết\n");
            suggestion.append("  - Tránh vay mượn hoặc dùng thẻ tín dụng");
        } else {
            suggestion.append("• Sát hết tháng - khuyến nghị:\n");
            suggestion.append("  - 🚨 CHỈ chi tiêu cho nhu cầu thiết yếu\n");
            suggestion.append("  - Chuẩn bị cho tháng mới\n");
            suggestion.append("  - Tổng kết và học từ các chi tiêu trong tháng");
        }

        return suggestion.toString();
    }

    private List<String> getGeneralSuggestions() {
        List<String> allSuggestions = List.of(
            "💡 Quy tắc 50/30/20:\n• 50% thu nhập cho nhu cầu thiết yếu\n• 30% cho mong muốn\n• 20% cho tiết kiệm",
            "🛒 Mẹo shopping thông minh:\n• Đợi 24h trước khi mua đồ đắt tiền\n• So sánh giá trên nhiều nền tảng\n• Tận dụng mã giảm giá và cashback",
            "☕ Tiết kiệm hàng ngày:\n• Pha cà phê tại nhà thay vì mua ngoài\n• Mang bình nước riêng\n• Tự nấu ăn ít nhất 5 bữa/tuần",
            "💳 Quản lý thẻ tín dụng:\n• Chỉ dùng nếu chắc chắn trả được\n• Thanh toán đầy đủ mỗi tháng\n• Tận dụng ưu đãi nhưng đừng mua vì có ưu đãi",
            "🎯 Mục tiêu tài chính:\n• Xây dựng quỹ khẩn cấp 3-6 tháng lương\n• Đầu tư học hỏi về tài chính\n• Review mục tiêu mỗi tháng",
            "🚫 Tránh bẫy chi tiêu:\n• Sale không phải lúc nào cũng tiết kiệm\n• Miễn phí ship khi mua không cần = lãng phí\n• Theo trend có thể làm hỏng ngân sách"
        );

        List<String> result = new ArrayList<>();
        Random random = new Random();

        // Chọn ngẫu nhiên 2-3 gợi ý
        int count = 2 + random.nextInt(2);
        List<String> shuffled = new ArrayList<>(allSuggestions);
        java.util.Collections.shuffle(shuffled);

        for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
            result.add(shuffled.get(i));
        }

        return result;
    }

    public String getPersonalizedSuggestion(Long userId, BigDecimal monthlyIncome, BigDecimal monthlySpending) {
        if (monthlyIncome == null || monthlySpending == null) {
            return "📈 Hãy cập nhật thu nhập và chi tiêu để nhận gợi ý cá nhân hóa!";
        }

        BigDecimal spendingRate = monthlySpending.divide(monthlyIncome, 2, BigDecimal.ROUND_HALF_UP)
                                                .multiply(BigDecimal.valueOf(100));

        StringBuilder suggestion = new StringBuilder("📊 Phân tích chi tiêu của bạn:\n\n");
        suggestion.append(String.format("Thu nhập: %,.0f VNĐ\n", monthlyIncome));
        suggestion.append(String.format("Chi tiêu: %,.0f VNĐ (%.1f%%)\n\n", monthlySpending, spendingRate));

        if (spendingRate.compareTo(BigDecimal.valueOf(90)) > 0) {
            suggestion.append("🚨 CẢNH BÁO: Bạn đang chi tiêu quá nhiều!\n");
            suggestion.append("• Cần giảm chi tiêu NGAY\n");
            suggestion.append("• Review tất cả khoản chi không cần thiết\n");
            suggestion.append("• Tìm cách tăng thu nhập\n");
        } else if (spendingRate.compareTo(BigDecimal.valueOf(70)) > 0) {
            suggestion.append("⚠️ Cẩn thận: Chi tiêu hơi cao\n");
            suggestion.append("• Cố gắng tiết kiệm ít nhất 20-30%\n");
            suggestion.append("• Xem xét các khoản chi có thể cắt giảm\n");
        } else if (spendingRate.compareTo(BigDecimal.valueOf(50)) > 0) {
            suggestion.append("✅ Tốt: Chi tiêu ở mức hợp lý\n");
            suggestion.append("• Duy trì tốt và cố gắng tiết kiệm thêm\n");
            suggestion.append("• Cân nhắc đầu tư số tiền tiết kiệm được\n");
        } else {
            suggestion.append("🌟 Xuất sắc: Bạn quản lý tài chính rất tốt!\n");
            suggestion.append("• Tiếp tục duy trì thói quen tốt này\n");
            suggestion.append("• Xem xét các kênh đầu tư để tiền sinh lời\n");
        }

        return suggestion.toString();
    }
}
