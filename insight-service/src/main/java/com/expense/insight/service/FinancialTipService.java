package com.expense.insight.service;

import com.expense.insight.entity.FinancialTip;
import com.expense.insight.repository.FinancialTipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinancialTipService {

    private final FinancialTipRepository tipRepository;

    @PostConstruct
    public void initializeTips() {
        if (tipRepository.count() == 0) {
            log.info("Initializing financial tips database...");
            createDefaultTips();
        }
    }

    public List<FinancialTip> getAllActiveTips() {
        return tipRepository.findByActiveOrderByPriorityDesc(true);
    }

    public List<FinancialTip> getTipsByCategory(FinancialTip.TipCategory category) {
        return tipRepository.findByCategoryAndActiveOrderByPriorityDesc(category, true);
    }

    public FinancialTip getRandomTip() {
        List<FinancialTip> tips = tipRepository.findRandomTips();
        if (tips.isEmpty()) {
            return null;
        }
        return tips.get(0);
    }

    public List<FinancialTip> getDailyTips(int count) {
        List<FinancialTip> tips = tipRepository.findRandomTips();
        return tips.stream().limit(count).collect(Collectors.toList());
    }

    private void createDefaultTips() {
        List<FinancialTip> tips = List.of(
            // SAVING Tips
            createTip("Quy tắc 50/30/20",
                "Chia thu nhập thành 3 phần: 50% cho chi phí thiết yếu (nhà, ăn, đi lại), 30% cho mong muốn (giải trí, mua sắm), và 20% cho tiết kiệm và đầu tư. Đây là công thức đơn giản nhưng hiệu quả để quản lý tài chính cá nhân.",
                FinancialTip.TipCategory.SAVING, 10),

            createTip("Tự động hóa tiết kiệm",
                "Thiết lập chuyển tiền tự động từ tài khoản chính sang tài khoản tiết kiệm ngay sau khi nhận lương. Phương pháp 'trả cho bản thân trước' này giúp bạn tiết kiệm đều đặn mà không cần ý chí mạnh mẽ.",
                FinancialTip.TipCategory.SAVING, 9),

            createTip("Challenge 52 tuần",
                "Tuần 1 tiết kiệm 10,000đ, tuần 2 tiết kiệm 20,000đ, cứ tăng dần như vậy. Cuối năm bạn sẽ có 13,780,000đ! Hoặc làm ngược lại, bắt đầu từ 520,000đ và giảm dần để dễ hơn vào cuối năm.",
                FinancialTip.TipCategory.SAVING, 8),

            // BUDGETING Tips
            createTip("Theo dõi chi tiêu hàng ngày",
                "Ghi chép MỌI khoản chi, dù nhỏ nhất. Sau 1 tháng, bạn sẽ nhận ra những khoản chi 'vô hình' như cà phê, trà sữa, đồ ăn vặt... chiếm đến 20-30% thu nhập. Awareness là bước đầu tiên để kiểm soát tài chính.",
                FinancialTip.TipCategory.BUDGETING, 10),

            createTip("Nguyên tắc 24 giờ",
                "Trước khi mua bất cứ thứ gì trên 500,000đ, hãy đợi 24 giờ. Với đồ trên 5 triệu, đợi 1 tuần. Điều này giúp tránh mua sắm cảm xúc và chỉ mua những gì thực sự cần thiết.",
                FinancialTip.TipCategory.BUDGETING, 9),

            createTip("Phong bì chi tiêu",
                "Chia tiền mặt vào các phong bì theo từng mục đích: ăn uống, đi lại, giải trí... Khi hết tiền trong phong bì nào thì dừng chi cho mục đó. Phương pháp cổ điển nhưng rất hiệu quả!",
                FinancialTip.TipCategory.BUDGETING, 7),

            // EMERGENCY_FUND Tips
            createTip("Quỹ khẩn cấp 3-6 tháng",
                "Ưu tiên xây dựng quỹ khẩn cấp đủ cho 3-6 tháng chi phí sinh hoạt TRƯỚC KHI đầu tư. Đây là tấm đệm tài chính giúp bạn vượt qua khó khăn mà không cần vay nợ.",
                FinancialTip.TipCategory.EMERGENCY_FUND, 10),

            createTip("Để quỹ khẩn cấp riêng",
                "Đặt quỹ khẩn cấp ở tài khoản RIÊNG, không dùng chung với tiền tiêu hàng ngày. Nên chọn tài khoản tiết kiệm online có lãi suất cao nhưng vẫn dễ rút khi cần.",
                FinancialTip.TipCategory.EMERGENCY_FUND, 8),

            // DEBT_MANAGEMENT Tips
            createTip("Tránh nợ tiêu dùng",
                "KHÔNG BAO GIỜ vay tiền (hoặc dùng thẻ tín dụng) để mua đồ tiêu dùng như quần áo, điện thoại, du lịch... Nếu không đủ tiền mặt để mua, nghĩa là bạn chưa đủ khả năng sở hữu.",
                FinancialTip.TipCategory.DEBT_MANAGEMENT, 10),

            createTip("Phương pháp Snowball",
                "Nếu có nhiều khoản nợ: trả tối thiểu cho tất cả, và dồn tiền để trả HẾT khoản nợ nhỏ nhất trước. Sau đó chuyển sang khoản tiếp theo. Chiến thắng nhỏ sẽ tạo động lực trả nợ.",
                FinancialTip.TipCategory.DEBT_MANAGEMENT, 8),

            // SHOPPING Tips
            createTip("So sánh giá thông minh",
                "Trước khi mua, check giá trên ít nhất 3 nền tảng khác nhau. Dùng các công cụ so sánh giá, theo dõi lịch sử giá. Nhiều khi 'sale' còn đắt hơn giá thường của shop khác!",
                FinancialTip.TipCategory.SHOPPING, 9),

            createTip("Mua theo nhu cầu, không theo sale",
                "Sale 50% cho thứ bạn không cần = lãng phí 50%. Chỉ mua khi: (1) Đã có trong kế hoạch mua, (2) Thực sự cần dùng, (3) Có trong ngân sách tháng này.",
                FinancialTip.TipCategory.SHOPPING, 10),

            createTip("Cashback và hoàn tiền",
                "Tận dụng các app/thẻ cashback, nhưng KHÔNG MUA chỉ vì có cashback. Hãy xem cashback như món quà bất ngờ, không phải lý do để mua sắm.",
                FinancialTip.TipCategory.SHOPPING, 7),

            // INVESTING Tips
            createTip("Bắt đầu đầu tư sớm",
                "Lãi kép là kỳ quan thứ 8 của thế giới. Đầu tư 1 triệu/tháng từ 25 tuổi (lãi 8%/năm) sẽ cho bạn 3.5 tỷ ở tuổi 50. Bắt đầu từ 35 tuổi chỉ cho 1.5 tỷ. Thời gian là tài sản lớn nhất!",
                FinancialTip.TipCategory.INVESTING, 10),

            createTip("Đa dạng hóa đầu tư",
                "Đừng để tất cả trứng vào một giỏ. Chia nhỏ tiền đầu tư vào nhiều kênh: tiết kiệm, vàng, chứng khoán, bất động sản... để giảm rủi ro.",
                FinancialTip.TipCategory.INVESTING, 8),

            // GENERAL Tips
            createTip("Tăng thu nhập thụ động",
                "Tìm cách tạo thu nhập thụ động: cho thuê phòng trọ, bán khóa học online, viết blog có quảng cáo, đầu tư cổ tức... Mục tiêu cuối cùng là tiền làm ra tiền!",
                FinancialTip.TipCategory.GENERAL, 8),

            createTip("Học về tài chính",
                "Đầu tư vào kiến thức tài chính là đầu tư sinh lợi nhất. Đọc sách, xem video, tham gia khóa học về quản lý tài chính cá nhân. Kiến thức không ai lấy được của bạn!",
                FinancialTip.TipCategory.GENERAL, 9),

            createTip("Review tài chính định kỳ",
                "Mỗi tháng dành 30 phút review: (1) Thu chi tháng này, (2) So với kế hoạch, (3) Điều chỉnh cho tháng sau. Mỗi quý review mục tiêu dài hạn.",
                FinancialTip.TipCategory.GENERAL, 10),

            createTip("Cà phê tại nhà",
                "Một ly cà phê 45,000đ mỗi ngày = 1,350,000đ/tháng = 16,200,000đ/năm! Pha cà phê tại nhà hoặc mang theo bình nước có thể tiết kiệm hàng chục triệu mỗi năm.",
                FinancialTip.TipCategory.GENERAL, 7),

            createTip("Meal prep tiết kiệm",
                "Chuẩn bị đồ ăn cho cả tuần vào Chủ Nhật. Ngoài tiết kiệm tiền (có thể giảm 50% chi phí ăn uống), còn tiết kiệm thời gian và ăn healthy hơn!",
                FinancialTip.TipCategory.GENERAL, 8),

            createTip("Chi tiêu có ý thức",
                "Trước mỗi lần chi tiêu, tự hỏi: (1) Tôi cần hay chỉ muốn? (2) Liệu tôi sẽ dùng nó thường xuyên? (3) Có phương án rẻ hơn không? (4) Mua nó có ảnh hưởng mục tiêu tài chính?",
                FinancialTip.TipCategory.GENERAL, 9)
        );

        tipRepository.saveAll(tips);
        log.info("Created {} financial tips", tips.size());
    }

    private FinancialTip createTip(String title, String content, FinancialTip.TipCategory category, int priority) {
        FinancialTip tip = new FinancialTip();
        tip.setTitle(title);
        tip.setContent(content);
        tip.setCategory(category);
        tip.setPriority(priority);
        tip.setActive(true);
        return tip;
    }

    public FinancialTip createCustomTip(String title, String content, FinancialTip.TipCategory category, Integer priority) {
        FinancialTip tip = new FinancialTip();
        tip.setTitle(title);
        tip.setContent(content);
        tip.setCategory(category);
        tip.setPriority(priority != null ? priority : 5);
        tip.setActive(true);
        return tipRepository.save(tip);
    }

    public FinancialTip updateTip(Long id, String title, String content, FinancialTip.TipCategory category, Integer priority) {
        FinancialTip tip = tipRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tip not found"));

        if (title != null) tip.setTitle(title);
        if (content != null) tip.setContent(content);
        if (category != null) tip.setCategory(category);
        if (priority != null) tip.setPriority(priority);

        return tipRepository.save(tip);
    }

    public void deleteTip(Long id) {
        tipRepository.deleteById(id);
    }
}
