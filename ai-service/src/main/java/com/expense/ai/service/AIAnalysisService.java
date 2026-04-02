package com.expense.ai.service;

import com.expense.ai.dto.ChatRequest;
import com.expense.ai.dto.ChatResponse;
import com.expense.ai.dto.SpendingAnalysis;
import com.expense.ai.dto.SpendingPrediction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    private final GeminiService geminiService;

    /**
     * Analyzes spending patterns and provides insights
     * Integrated with Google Gemini API for intelligent responses
     */
    public SpendingAnalysis analyzeSpending(Long userId, Map<String, Object> transactionData) {
        log.info("Analyzing spending for user: {}", userId);
        log.debug("Transaction data: {}", transactionData);

        // Extract data - handle both Integer and Double
        Double totalIncome = convertToDouble(transactionData.getOrDefault("totalIncome", 0.0));
        Double totalExpense = convertToDouble(transactionData.getOrDefault("totalExpense", 0.0));
        @SuppressWarnings("unchecked")
        Map<String, Object> rawCategoryBreakdown = (Map<String, Object>)
            transactionData.getOrDefault("categoryBreakdown", new HashMap<>());

        // Convert category amounts to Double
        Map<String, Double> categoryBreakdown = new HashMap<>();
        rawCategoryBreakdown.forEach((key, value) ->
            categoryBreakdown.put(key, convertToDouble(value))
        );

        // Calculate metrics
        Double savingsRate = totalIncome > 0 ? ((totalIncome - totalExpense) / totalIncome) * 100 : 0.0;

        // Get top spending categories
        List<String> topCategories = categoryBreakdown.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        // Generate insights
        List<String> insights = generateInsights(totalIncome, totalExpense, savingsRate, categoryBreakdown);

        // Generate recommendations
        List<String> recommendations = generateRecommendations(savingsRate, categoryBreakdown);

        // Determine financial health
        String financialHealth = determineFinancialHealth(savingsRate);

        return SpendingAnalysis.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .savingsRate(savingsRate)
                .categoryBreakdown(categoryBreakdown)
                .topSpendingCategories(topCategories)
                .insights(insights)
                .recommendations(recommendations)
                .financialHealth(financialHealth)
                .build();
    }

    public SpendingPrediction predictSpending(Long userId, Map<String, Object> historicalData) {
        log.info("Predicting spending for user: {} - Dự đoán dựa trên tháng gần nhất", userId);
        log.debug("Historical data: {}", historicalData);

        // CẢI TIẾN: Dự đoán dựa trên THÁNG GẦN NHẤT thay vì trung bình
        // Phản ánh chính xác hơn thói quen chi tiêu hiện tại

        try {
            // Safely extract and convert past expenses
            List<Double> pastExpenses = new ArrayList<>();
            Object pastExpensesObj = historicalData.get("pastExpenses");
            if (pastExpensesObj instanceof List<?>) {
                for (Object obj : (List<?>) pastExpensesObj) {
                    pastExpenses.add(convertToDouble(obj));
                }
            }

            // Safely extract and convert category history
            Map<String, Double> categoryPredictions = new HashMap<>();
            Object categoryHistoryObj = historicalData.get("categoryHistory");
            if (categoryHistoryObj instanceof Map<?, ?>) {
                Map<?, ?> categoryMap = (Map<?, ?>) categoryHistoryObj;
                categoryMap.forEach((key, value) -> {
                    if (key instanceof String) {
                        // CẢI TIẾN: Dựa trên tháng gần nhất + tăng trưởng 3%
                        categoryPredictions.put((String) key, convertToDouble(value) * 1.03);
                    }
                });
            }

            // CẢI TIẾN: Sử dụng chi tiêu THÁNG GẦN NHẤT làm cơ sở dự đoán
            double latestMonthExpense = 0.0;
            double avgExpense = 0.0;

            if (!pastExpenses.isEmpty()) {
                // Lấy chi tiêu tháng gần nhất (phần tử cuối cùng)
                latestMonthExpense = pastExpenses.get(pastExpenses.size() - 1);
                avgExpense = pastExpenses.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            }

            // Dự đoán = Tháng gần nhất + 3% (thay vì trung bình + 5%)
            double predictedExpense = latestMonthExpense > 0 ? latestMonthExpense * 1.03 : 0.0;

            // Determine trend based on latest vs average
            String trend = determineTrendFromLatest(pastExpenses, latestMonthExpense, avgExpense);

            // Generate warnings based on latest month
            List<String> warnings = generateWarningsFromLatest(predictedExpense, latestMonthExpense, avgExpense);

            return SpendingPrediction.builder()
                    .predictedNextMonthExpense(predictedExpense)
                    .categoryPredictions(categoryPredictions)
                    .confidenceLevel(pastExpenses.isEmpty() ? 0.0 : 80.0) // Tăng confidence vì dựa trên dữ liệu gần nhất
                    .trend(trend)
                    .warnings(warnings)
                    .build();
        } catch (Exception e) {
            log.error("Error predicting spending for user {}: {}", userId, e.getMessage(), e);
            // Return a safe default prediction if parsing fails
            return SpendingPrediction.builder()
                    .predictedNextMonthExpense(0.0)
                    .categoryPredictions(new HashMap<>())
                    .confidenceLevel(0.0)
                    .trend("ỔN ĐỊNH")
                    .warnings(Arrays.asList("Không đủ dữ liệu để dự đoán chính xác. Hãy thêm giao dịch để nhận dự đoán tốt hơn!"))
                    .build();
        }
    }

    public ChatResponse chat(Long userId, ChatRequest request) {
        log.info("Processing chat request for user: {}", userId);

        String userMessage = request.getMessage();

        // Context tùy chọn - chỉ thêm nếu câu hỏi liên quan đến tài chính
        String context = "";
        String lowerMessage = userMessage.toLowerCase();
        if (lowerMessage.contains("tiết kiệm") || lowerMessage.contains("chi tiêu") ||
            lowerMessage.contains("ngân sách") || lowerMessage.contains("đầu tư") ||
            lowerMessage.contains("tiền") || lowerMessage.contains("tài chính")) {
            context = "Người dùng đang sử dụng ứng dụng quản lý chi tiêu cá nhân.";
        }

        // Gọi Gemini API - có thể trả lời mọi câu hỏi
        String geminiResponse = geminiService.chat(userMessage, context);

        // Xác định category dựa trên nội dung
        String category = categorizeMessage(userMessage);

        return ChatResponse.builder()
                .message(geminiResponse)
                .category(category)
                .build();
    }

    /**
     * Phân loại tin nhắn để tracking
     */
    private String categorizeMessage(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("xin chào") || lowerMessage.contains("chào") ||
            lowerMessage.contains("hi") || lowerMessage.contains("hello")) {
            return "GREETING";
        } else if (lowerMessage.contains("tiết kiệm") || lowerMessage.contains("tiet kiem")) {
            return "SAVINGS_TIP";
        } else if (lowerMessage.contains("ngân sách") || lowerMessage.contains("ngan sach")) {
            return "BUDGET_SUGGESTION";
        } else if (lowerMessage.contains("chi tiêu") || lowerMessage.contains("chi tieu")) {
            return "EXPENSE_ANALYSIS";
        } else if (lowerMessage.contains("đầu tư") || lowerMessage.contains("dau tu")) {
            return "INVESTMENT_ADVICE";
        } else if (lowerMessage.contains("nợ") || lowerMessage.contains("vay")) {
            return "DEBT_MANAGEMENT";
        } else if (lowerMessage.contains("thu nhập") || lowerMessage.contains("thu nhap")) {
            return "INCOME_ADVICE";
        } else {
            return "GENERAL";
        }
    }

    // LEGACY FALLBACK CODE - Giữ lại để tham khảo nếu Gemini API không hoạt động
    @Deprecated
    private ChatResponse chatFallback(Long userId, ChatRequest request) {
        String message = request.getMessage().toLowerCase();
        String response;
        String category;

        // Greetings
        if (message.contains("xin chào") || message.contains("chào") || message.contains("hi") || message.contains("hello")) {
            response = "Xin chào! Tôi là trợ lý tài chính AI của bạn. Tôi có thể giúp bạn:\n" +
                      "💰 Phân tích chi tiêu và thu nhập\n" +
                      "📊 Lập kế hoạch ngân sách\n" +
                      "🎯 Đặt và theo dõi mục tiêu tiết kiệm\n" +
                      "📈 Dự đoán xu hướng tài chính\n" +
                      "💡 Đưa ra lời khuyên cá nhân hóa\n\n" +
                      "Bạn muốn bắt đầu từ đâu?";
            category = "GREETING";
        }
        // Savings advice
        else if (message.contains("tiết kiệm") || message.contains("tiet kiem") || message.contains("save") || message.contains("saving")) {
            response = "💰 **Chiến lược tiết kiệm hiệu quả:**\n\n" +
                      "1️⃣ **Quy tắc 50/30/20:** 50% nhu cầu thiết yếu, 30% mong muốn, 20% tiết kiệm\n" +
                      "2️⃣ **Tự động hóa:** Thiết lập chuyển khoản tự động vào tài khoản tiết kiệm\n" +
                      "3️⃣ **Cắt giảm chi phí:** Xem xét các khoản đăng ký không cần thiết\n" +
                      "4️⃣ **Thử thách tiết kiệm:** Bắt đầu với 52-week saving challenge\n" +
                      "5️⃣ **Quỹ khẩn cấp:** Tiết kiệm 3-6 tháng chi phí sinh hoạt\n\n" +
                      "💡 **Mẹo:** Mỗi khi nhận lương, hãy tiết kiệm trước khi chi tiêu!";
            category = "SAVINGS_TIP";
        }
        // Budget planning
        else if (message.contains("ngân sách") || message.contains("ngan sach") || message.contains("budget")) {
            response = "📊 **Hướng dẫn lập ngân sách chi tiết:**\n\n" +
                      "**Bước 1: Tính thu nhập**\n" +
                      "• Tổng hợp tất cả nguồn thu nhập hàng tháng\n\n" +
                      "**Bước 2: Theo dõi chi tiêu**\n" +
                      "• Chi phí cố định: Nhà, điện nước, bảo hiểm\n" +
                      "• Chi phí biến động: Ăn uống, giải trí, mua sắm\n\n" +
                      "**Bước 3: Phân loại**\n" +
                      "• Sử dụng app này để tự động phân loại\n" +
                      "• Xem báo cáo để hiểu rõ thói quen chi tiêu\n\n" +
                      "**Bước 4: Đặt giới hạn**\n" +
                      "• Tạo ngân sách cho từng danh mục\n" +
                      "• Nhận cảnh báo khi vượt ngưỡng\n\n" +
                      "**Bước 5: Điều chỉnh**\n" +
                      "• Xem xét hàng tháng và tối ưu hóa";
            category = "BUDGET_SUGGESTION";
        }
        // Expense reduction
        else if (message.contains("chi tiêu") || message.contains("chi tieu") || message.contains("giảm chi") || message.contains("expense") || message.contains("spending")) {
            response = "💸 **Cách giảm chi tiêu thông minh:**\n\n" +
                      "**Ăn uống:**\n" +
                      "• Nấu ăn tại nhà thay vì ăn ngoài\n" +
                      "• Lên kế hoạch bữa ăn trong tuần\n" +
                      "• Mua sắm theo danh sách, tránh mua impulsive\n\n" +
                      "**Giải trí:**\n" +
                      "• Tìm hoạt động miễn phí: công viên, bảo tàng\n" +
                      "• Hủy các đăng ký không sử dụng\n\n" +
                      "**Mua sắm:**\n" +
                      "• Áp dụng quy tắc 24h trước khi mua\n" +
                      "• So sánh giá trước khi quyết định\n" +
                      "• Tìm mã giảm giá, ưu đãi\n\n" +
                      "**Giao thông:**\n" +
                      "• Sử dụng phương tiện công cộng\n" +
                      "• Carpool với đồng nghiệp";
            category = "EXPENSE_ANALYSIS";
        }
        // Investment advice
        else if (message.contains("đầu tư") || message.contains("dau tu") || message.contains("invest")) {
            response = "📈 **Hướng dẫn đầu tư cho người mới:**\n\n" +
                      "**Nguyên tắc cơ bản:**\n" +
                      "1️⃣ Chỉ đầu tư số tiền bạn có thể chấp nhận mất\n" +
                      "2️⃣ Đa dạng hóa danh mục đầu tư\n" +
                      "3️⃣ Đầu tư dài hạn, kiên nhẫn\n\n" +
                      "**Các kênh đầu tư phổ biến:**\n" +
                      "💰 **Tiết kiệm ngân hàng:** An toàn, lợi nhuận thấp\n" +
                      "📊 **Cổ phiếu:** Rủi ro cao, tiềm năng lợi nhuận cao\n" +
                      "🏠 **Bất động sản:** Cần vốn lớn, ổn định\n" +
                      "📜 **Trái phiếu:** Rủi ro trung bình\n\n" +
                      "⚠️ **Lưu ý:** Học hỏi kiến thức trước khi đầu tư!";
            category = "INVESTMENT_ADVICE";
        }
        // Debt management
        else if (message.contains("nợ") || message.contains("vay") || message.contains("debt") || message.contains("loan")) {
            response = "💳 **Quản lý và giảm nợ hiệu quả:**\n\n" +
                      "**Phương pháp Snowball:**\n" +
                      "• Trả nợ nhỏ nhất trước để có động lực\n" +
                      "• Sau đó chuyển sang khoản nợ tiếp theo\n\n" +
                      "**Phương pháp Avalanche:**\n" +
                      "• Ưu tiên khoản nợ lãi suất cao nhất\n" +
                      "• Tiết kiệm nhiều tiền lãi hơn\n\n" +
                      "**Mẹo quan trọng:**\n" +
                      "✅ Trả nhiều hơn mức tối thiểu mỗi tháng\n" +
                      "✅ Tránh vay nợ mới trong quá trình trả nợ\n" +
                      "✅ Xem xét tái cấu trúc nếu lãi suất quá cao\n" +
                      "✅ Tạo ngân sách chặt chẽ để có thêm tiền trả nợ\n\n" +
                      "💡 **Mục tiêu:** Sống không nợ nần!";
            category = "DEBT_MANAGEMENT";
        }
        // Income increase
        else if (message.contains("thu nhập") || message.contains("thu nhap") || message.contains("kiếm tiền") || message.contains("kiem tien") || message.contains("income")) {
            response = "💼 **Cách tăng thu nhập:**\n\n" +
                      "**Thu nhập chính:**\n" +
                      "• Xin tăng lương/thăng chức\n" +
                      "• Nâng cao kỹ năng chuyên môn\n" +
                      "• Tìm công việc lương cao hơn\n\n" +
                      "**Thu nhập phụ:**\n" +
                      "• Freelance: Thiết kế, lập trình, viết lách\n" +
                      "• Kinh doanh online\n" +
                      "• Cho thuê tài sản nhàn rỗi\n" +
                      "• Làm thêm part-time\n\n" +
                      "**Thu nhập thụ động:**\n" +
                      "• Đầu tư cổ tức\n" +
                      "• Cho thuê nhà/phòng\n" +
                      "• Bản quyền (sách, khóa học)\n" +
                      "• Affiliate marketing\n\n" +
                      "💡 **Lưu ý:** Đừng quên cân bằng công việc và cuộc sống!";
            category = "INCOME_ADVICE";
        }
        // Financial goals
        else if (message.contains("mục tiêu") || message.contains("muc tieu") || message.contains("goal") || message.contains("kế hoạch") || message.contains("ke hoach")) {
            response = "🎯 **Thiết lập mục tiêu tài chính SMART:**\n\n" +
                      "**S - Specific (Cụ thể):**\n" +
                      "Ví dụ: 'Tiết kiệm 100 triệu' thay vì 'Tiết kiệm nhiều tiền'\n\n" +
                      "**M - Measurable (Đo lường được):**\n" +
                      "Theo dõi tiến độ bằng app này\n\n" +
                      "**A - Achievable (Khả thi):**\n" +
                      "Đặt mục tiêu phù hợp với thu nhập\n\n" +
                      "**R - Relevant (Liên quan):**\n" +
                      "Phù hợp với hoàn cảnh và ưu tiên của bạn\n\n" +
                      "**T - Time-bound (Có thời hạn):**\n" +
                      "Đặt deadline rõ ràng\n\n" +
                      "**Ví dụ mục tiêu tốt:**\n" +
                      "• Tiết kiệm 50 triệu trong 12 tháng để du lịch\n" +
                      "• Trả hết nợ thẻ tín dụng trong 6 tháng\n" +
                      "• Đầu tư 5 triệu/tháng vào quỹ hưu trí";
            category = "GOAL_SETTING";
        }
        // Emergency fund
        else if (message.contains("khẩn cấp") || message.contains("khan cap") || message.contains("emergency") || message.contains("dự phòng")) {
            response = "🚨 **Quỹ khẩn cấp - Tại sao quan trọng:**\n\n" +
                      "**Quỹ khẩn cấp là gì?**\n" +
                      "Khoản tiền dành riêng cho các tình huống bất ngờ:\n" +
                      "• Mất việc làm\n" +
                      "• Ốm đau, y tế\n" +
                      "• Sửa chữa nhà cửa, xe cộ\n" +
                      "• Việc gia đình khẩn cấp\n\n" +
                      "**Nên tiết kiệm bao nhiêu?**\n" +
                      "💰 Tối thiểu: 3-6 tháng chi phí sinh hoạt\n" +
                      "💰 Lý tưởng: 6-12 tháng nếu công việc không ổn định\n\n" +
                      "**Cách xây dựng:**\n" +
                      "1️⃣ Bắt đầu với mục tiêu nhỏ: 5-10 triệu\n" +
                      "2️⃣ Tự động chuyển tiền mỗi tháng\n" +
                      "3️⃣ Để riêng, không động vào trừ khẩn cấp\n" +
                      "4️⃣ Gửi tiết kiệm ngắn hạn, dễ rút\n\n" +
                      "⚠️ **Quan trọng:** Đây là lưới an toàn tài chính!";
            category = "EMERGENCY_FUND";
        }
        // Tax advice
        else if (message.contains("thuế") || message.contains("thue") || message.contains("tax")) {
            response = "📝 **Tối ưu hóa thuế cá nhân:**\n\n" +
                      "**Giảm trừ gia cảnh:**\n" +
                      "• Bản thân: 11 triệu/tháng\n" +
                      "• Người phụ thuộc: 4.4 triệu/người/tháng\n\n" +
                      "**Các khoản được trừ:**\n" +
                      "✅ Bảo hiểm bắt buộc\n" +
                      "✅ Bảo hiểm tự nguyện (trong hạn mức)\n" +
                      "✅ Từ thiện cho tổ chức hợp pháp\n" +
                      "✅ Quỹ học bổng\n\n" +
                      "**Mẹo tiết kiệm thuế:**\n" +
                      "• Khai đủ người phụ thuộc\n" +
                      "• Đăng ký bảo hiểm hưu trí tự nguyện\n" +
                      "• Lưu giữ chứng từ đóng góp từ thiện\n" +
                      "• Khai báo đúng hạn để tránh phạt\n\n" +
                      "💡 **Lưu ý:** Tham khảo chuyên gia thuế cho tình huống cụ thể!";
            category = "TAX_ADVICE";
        }
        // Retirement planning
        else if (message.contains("hưu trí") || message.contains("huu tri") || message.contains("retirement") || message.contains("về già")) {
            response = "👴 **Lập kế hoạch hưu trí:**\n\n" +
                      "**Công thức 4% Rule:**\n" +
                      "Cần tiết kiệm 25x chi phí hàng năm khi về hưu\n" +
                      "Ví dụ: Chi 20 triệu/tháng = 240tr/năm → Cần 6 tỷ\n\n" +
                      "**Nguồn thu hưu trí:**\n" +
                      "1️⃣ Lương hưu nhà nước\n" +
                      "2️⃣ Tiết kiệm cá nhân\n" +
                      "3️⃣ Đầu tư dài hạn\n" +
                      "4️⃣ Bảo hiểm hưu trí\n" +
                      "5️⃣ Thu nhập thụ động\n\n" +
                      "**Bắt đầu từ sớm:**\n" +
                      "• Tuổi 25-35: Tiết kiệm 15-20% thu nhập\n" +
                      "• Tuổi 35-45: Tăng lên 20-25%\n" +
                      "• Tuổi 45-55: 25-30%\n\n" +
                      "💰 **Lợi thế lãi kép:** Càng sớm càng tốt!\n\n" +
                      "📊 Sử dụng tính năng 'Heo Đất Ảo' để theo dõi tiến độ!";
            category = "RETIREMENT_PLANNING";
        }
        // Default response
        else {
            response = "🤖 **Trợ lý tài chính AI**\n\n" +
                      "Tôi có thể giúp bạn về:\n\n" +
                      "💰 **Tiết kiệm:** Chiến lược tiết kiệm hiệu quả\n" +
                      "📊 **Ngân sách:** Lập và quản lý ngân sách\n" +
                      "💸 **Chi tiêu:** Giảm chi phí không cần thiết\n" +
                      "📈 **Đầu tư:** Hướng dẫn đầu tư cơ bản\n" +
                      "💳 **Quản lý nợ:** Chiến lược trả nợ\n" +
                      "💼 **Thu nhập:** Cách tăng thu nhập\n" +
                      "🎯 **Mục tiêu:** Thiết lập mục tiêu tài chính\n" +
                      "🚨 **Quỹ khẩn cấp:** Xây dựng lưới an toàn\n" +
                      "📝 **Thuế:** Tối ưu hóa thuế cá nhân\n" +
                      "👴 **Hưu trí:** Lập kế hoạch về già\n\n" +
                      "Hãy hỏi tôi bất cứ điều gì về tài chính!";
            category = "GENERAL_ADVICE";
        }

        return ChatResponse.builder()
                .message(response)
                .category(category)
                .build();
    }

    // Helper methods
    private List<String> generateInsights(Double income, Double expense, Double savingsRate,
                                         Map<String, Double> categories) {
        List<String> insights = new ArrayList<>();

        if (savingsRate < 10) {
            insights.add("Tỷ lệ tiết kiệm của bạn dưới 10%, điều này đáng lo ngại cho sức khỏe tài chính lâu dài.");
        } else if (savingsRate > 20) {
            insights.add("Làm tốt lắm! Bạn đang tiết kiệm hơn 20% thu nhập của mình.");
        }

        if (expense > income) {
            insights.add("Cảnh báo: Chi tiêu của bạn vượt quá thu nhập. Điều này không bền vững.");
        }

        // Category-specific insights
        categories.forEach((category, amount) -> {
            double percentage = (amount / expense) * 100;
            if (percentage > 30) {
                insights.add(String.format("%s chiếm %.1f%% tổng chi tiêu, hãy xem xét lại danh mục này.",
                    category, percentage));
            }
        });

        return insights;
    }

    private List<String> generateRecommendations(Double savingsRate, Map<String, Double> categories) {
        List<String> recommendations = new ArrayList<>();

        if (savingsRate < 20) {
            recommendations.add("Hãy cố gắng tăng tỷ lệ tiết kiệm lên ít nhất 20% thu nhập.");
        }

        recommendations.add("Thiết lập chuyển tiền tự động vào tài khoản tiết kiệm vào ngày lương.");
        recommendations.add("Xem xét và hủy các dịch vụ đăng ký không sử dụng.");
        recommendations.add("Tạo mục tiêu heo đất để giữ động lực.");

        return recommendations;
    }

    private String determineFinancialHealth(Double savingsRate) {
        if (savingsRate >= 30) return "XUẤT SẮC";
        if (savingsRate >= 20) return "TỐT";
        if (savingsRate >= 10) return "TRUNG BÌNH";
        return "YẾU";
    }

    private String determineTrend(List<Double> pastExpenses) {
        if (pastExpenses.size() < 2) return "ỔN ĐỊNH";

        double first = pastExpenses.get(0);
        double last = pastExpenses.get(pastExpenses.size() - 1);

        if (last > first * 1.1) return "TĂNG";
        if (last < first * 0.9) return "GIẢM";
        return "ỔN ĐỊNH";
    }

    // CẢI TIẾN: Phương thức mới để xác định xu hướng dựa trên tháng gần nhất
    private String determineTrendFromLatest(List<Double> pastExpenses, double latestMonth, double average) {
        if (pastExpenses.size() < 2) return "ỔN ĐỊNH";

        // So sánh tháng gần nhất với trung bình
        if (latestMonth > average * 1.15) return "TĂNG CAO";
        if (latestMonth > average * 1.05) return "TĂNG";
        if (latestMonth < average * 0.85) return "GIẢM NHIỀU";
        if (latestMonth < average * 0.95) return "GIẢM";
        return "ỔN ĐỊNH";
    }

    private List<String> generateWarnings(Double predicted, Double average) {
        List<String> warnings = new ArrayList<>();

        if (predicted > average * 1.2) {
            warnings.add("Chi tiêu dự đoán sẽ tăng đáng kể vào tháng tới.");
        }

        return warnings;
    }

    // CẢI TIẾN: Cảnh báo dựa trên tháng gần nhất
    private List<String> generateWarningsFromLatest(Double predicted, Double latestMonth, Double average) {
        List<String> warnings = new ArrayList<>();

        if (latestMonth > average * 1.2) {
            warnings.add(String.format("⚠️ CẢNH BÁO: Chi tiêu tháng vừa qua (%.0f VND) cao hơn trung bình %.1f%%. Hãy kiểm soát chi tiêu!",
                    latestMonth, ((latestMonth / average - 1) * 100)));
        }

        if (predicted > latestMonth * 1.1) {
            warnings.add(String.format("📊 Dự đoán tháng tới sẽ tăng %.1f%% so với tháng này. Chuẩn bị ngân sách kỹ!",
                    ((predicted / latestMonth - 1) * 100)));
        }

        double savingsRate = average > 0 ? ((1 - latestMonth / average) * 100) : 0;
        if (savingsRate < 10) {
            warnings.add("💡 Mẹo: Hãy cố gắng tiết kiệm ít nhất 10-20% thu nhập mỗi tháng để có tương lai tài chính ổn định!");
        }

        return warnings;
    }

    /**
     * Convert Object to Double, handling both Integer and Double types
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * CẢI TIẾN: Phân tích thống kê với dự đoán dựa trên THÁNG GẦN NHẤT
     * và gợi ý ngân sách dựa trên THU NHẬP THỰC TẾ của tháng hiện tại
     */
    public com.expense.ai.dto.StatisticsResponse analyzeStatistics(Long userId, com.expense.ai.dto.StatisticsRequest request) {
        List<com.expense.ai.dto.StatisticsRequest.MonthlyData> monthlyData = request.getMonthlyData();

        if (monthlyData == null || monthlyData.isEmpty()) {
            // Return empty response if no data
            return com.expense.ai.dto.StatisticsResponse.builder()
                    .insights(List.of("📊 Chưa có dữ liệu để phân tích. Hãy thêm giao dịch thu nhập và chi tiêu để nhận được dự đoán chính xác!"))
                    .warnings(new ArrayList<>())
                    .budgetSuggestions(new ArrayList<>())
                    .build();
        }

        // CẢI TIẾN: Lấy dữ liệu THÁNG GẦN NHẤT làm cơ sở
        com.expense.ai.dto.StatisticsRequest.MonthlyData latestMonth = monthlyData.get(monthlyData.size() - 1);

        // Thu nhập và chi tiêu tháng gần nhất
        double latestIncome = latestMonth.getTotalIncome() != null ? latestMonth.getTotalIncome() : 0.0;
        double latestExpense = latestMonth.getTotalExpense() != null ? latestMonth.getTotalExpense() : 0.0;

        // Tính trung bình để so sánh xu hướng
        double avgIncome = monthlyData.stream()
                .mapToDouble(m -> m.getTotalIncome() != null ? m.getTotalIncome() : 0.0)
                .average().orElse(0.0);
        double avgExpense = monthlyData.stream()
                .mapToDouble(m -> m.getTotalExpense() != null ? m.getTotalExpense() : 0.0)
                .average().orElse(0.0);

        // CẢI TIẾN: Dự đoán dựa trên THÁNG GẦN NHẤT + xu hướng
        double trend = calculateTrend(monthlyData);

        // Dự đoán chi tiêu: Tháng gần nhất + 3% (thực tế hơn)
        double predictedExpense = latestExpense * 1.03;

        // CẢI TIẾN: Dự đoán thu nhập dựa trên THU NHẬP THỰC TẾ THÁNG NÀY
        // Giả định thu nhập ổn định (không tăng/giảm đột ngột)
        double predictedIncome = latestIncome > 0 ? latestIncome : avgIncome;

        // CẢI TIẾN: Dự đoán chi tiêu theo danh mục dựa trên THÁNG GẦN NHẤT
        Map<String, Double> categoryPredictions = new HashMap<>();
        if (latestMonth.getCategoryExpenses() != null) {
            latestMonth.getCategoryExpenses().forEach((category, amount) -> {
                // Dự đoán = Chi tiêu danh mục tháng gần nhất + 3%
                categoryPredictions.put(category, amount * 1.03);
            });
        }

        // Tiềm năng tiết kiệm
        double savingsPotential = predictedIncome - predictedExpense;

        // Tạo dự đoán
        com.expense.ai.dto.StatisticsResponse.PredictionData prediction =
                com.expense.ai.dto.StatisticsResponse.PredictionData.builder()
                        .period("Tháng tới")
                        .predictedIncome(predictedIncome)
                        .predictedExpense(predictedExpense)
                        .categoryPredictions(categoryPredictions)
                        .savingsPotential(savingsPotential)
                        .build();

        // CẢI TIẾN: Gợi ý ngân sách dựa trên THU NHẬP THỰC TẾ THÁNG NÀY
        List<com.expense.ai.dto.StatisticsResponse.BudgetSuggestion> budgetSuggestions =
                generateBudgetSuggestions(predictedIncome, latestIncome, categoryPredictions, latestMonth.getCategoryExpenses());

        // Tạo insights dựa trên tháng gần nhất
        List<String> insights = generateStatisticsInsightsFromLatest(monthlyData, latestIncome, latestExpense,
                avgIncome, avgExpense, trend, savingsPotential);

        // Cảnh báo dựa trên tháng gần nhất
        List<String> warnings = generateStatisticsWarningsFromLatest(trend, savingsPotential, latestExpense,
                avgExpense, latestIncome);

        return com.expense.ai.dto.StatisticsResponse.builder()
                .prediction(prediction)
                .budgetSuggestions(budgetSuggestions)
                .insights(insights)
                .warnings(warnings)
                .build();
    }

    private double calculateTrend(List<com.expense.ai.dto.StatisticsRequest.MonthlyData> monthlyData) {
        if (monthlyData.size() < 2) return 0.0;

        // Simple trend: compare last month to average
        double lastMonthExpense = monthlyData.get(monthlyData.size() - 1).getTotalExpense();
        double avgExpense = monthlyData.stream()
                .limit(monthlyData.size() - 1)
                .mapToDouble(m -> m.getTotalExpense() != null ? m.getTotalExpense() : 0.0)
                .average().orElse(lastMonthExpense);

        return avgExpense > 0 ? (lastMonthExpense - avgExpense) / avgExpense : 0.0;
    }

    /**
     * CẢI TIẾN V2: Tạo gợi ý ngân sách thông minh đảm bảo tiết kiệm 10-20%
     * Sử dụng quy tắc 50/30/20 cải tiến:
     * - 10-20% tiết kiệm (ưu tiên cao nhất)
     * - 50% nhu cầu thiết yếu (ăn uống, nhà ở, giao thông, y tế)
     * - 30% chi tiêu linh hoạt (giáo dục, giải trí, mua sắm, khác)
     */
    private List<com.expense.ai.dto.StatisticsResponse.BudgetSuggestion> generateBudgetSuggestions(
            double predictedIncome, double currentIncome, Map<String, Double> categoryPredictions,
            Map<String, Double> currentCategoryExpenses) {

        List<com.expense.ai.dto.StatisticsResponse.BudgetSuggestion> suggestions = new ArrayList<>();

        // Sử dụng thu nhập thực tế tháng này làm cơ sở
        double baseIncome = currentIncome > 0 ? currentIncome : predictedIncome;
        double totalPredicted = categoryPredictions.values().stream().mapToDouble(d -> d).sum();

        // MỤC TIÊU: Tiết kiệm 15% thu nhập (nằm trong khoảng 10-20%)
        final double targetSavingsRate = 0.15; // 15%
        final double targetSavings = baseIncome * targetSavingsRate;

        // PHÂN BỔ CHI TIÊU: 85% thu nhập còn lại
        final double totalBudget = baseIncome - targetSavings; // 85% thu nhập

        // Phân loại danh mục theo tính chất
        final double essentialBudget = totalBudget * 0.60;  // 60% của 85% = ~51% thu nhập cho thiết yếu
        final double flexibleBudget = totalBudget * 0.40;   // 40% của 85% = ~34% thu nhập cho linh hoạt

        log.info("Thu nhập: {}, Mục tiêu tiết kiệm 15%: {}, Ngân sách chi tiêu: {} (Thiết yếu: {}, Linh hoạt: {})",
                baseIncome, targetSavings, totalBudget, essentialBudget, flexibleBudget);

        // Kiểm tra xem chi tiêu dự đoán có vượt quá ngân sách không
        final boolean needAdjustment = totalPredicted > totalBudget;

        // Phân loại danh mục: Thiết yếu vs Linh hoạt
        Map<String, String> categoryType = Map.of(
                "ĂN UỐNG", "THIẾT YẾU",
                "NHÀ Ở", "THIẾT YẾU",
                "GIAO THÔNG", "THIẾT YẾU",
                "Y TẾ", "THIẾT YẾU",
                "GIÁO DỤC", "LINH HOẠT",
                "MUA SẮM", "LINH HOẠT",
                "GIẢI TRÍ", "LINH HOẠT",
                "KHÁC", "LINH HOẠT"
        );

        // Tính tổng chi tiêu dự đoán cho từng loại
        double totalEssentialPredicted = categoryPredictions.entrySet().stream()
                .filter(e -> "THIẾT YẾU".equals(categoryType.getOrDefault(e.getKey(), "LINH HOẠT")))
                .mapToDouble(Map.Entry::getValue)
                .sum();

        double totalFlexiblePredicted = categoryPredictions.entrySet().stream()
                .filter(e -> "LINH HOẠT".equals(categoryType.getOrDefault(e.getKey(), "LINH HOẠT")))
                .mapToDouble(Map.Entry::getValue)
                .sum();

        // Hệ số điều chỉnh để đảm bảo không vượt ngân sách
        double essentialAdjustment = totalEssentialPredicted > 0 ?
                Math.min(1.0, essentialBudget / totalEssentialPredicted) : 1.0;
        double flexibleAdjustment = totalFlexiblePredicted > 0 ?
                Math.min(1.0, flexibleBudget / totalFlexiblePredicted) : 1.0;

        log.info("Chi tiêu thiết yếu dự đoán: {} (ngân sách: {}, hệ số: {})",
                totalEssentialPredicted, essentialBudget, essentialAdjustment);
        log.info("Chi tiêu linh hoạt dự đoán: {} (ngân sách: {}, hệ số: {})",
                totalFlexiblePredicted, flexibleBudget, flexibleAdjustment);

        // Tạo gợi ý cho từng danh mục
        categoryPredictions.forEach((category, predicted) -> {
            String type = categoryType.getOrDefault(category, "LINH HOẠT");
            boolean isEssential = "THIẾT YẾU".equals(type);

            // Áp dụng hệ số điều chỉnh phù hợp
            double adjustment = isEssential ? essentialAdjustment : flexibleAdjustment;
            double suggested = predicted * adjustment;

            // Tính % so với tổng chi tiêu
            double percentageOfTotal = totalPredicted > 0 ? (predicted / totalPredicted) * 100 : 0;

            // Tính % so với thu nhập
            double percentageOfIncome = baseIncome > 0 ? (suggested / baseIncome) * 100 : 0;

            // Chi tiêu hiện tại
            double currentAmount = currentCategoryExpenses != null ?
                    currentCategoryExpenses.getOrDefault(category, predicted) : predicted;

            String reason;

            // LOGIC GỢI Ý THÔNG MINH
            if (needAdjustment && adjustment < 1.0) {
                // Cần giảm chi tiêu để đảm bảo tiết kiệm
                double reductionPercent = (1 - adjustment) * 100;
                reason = String.format("💰 Để đảm bảo tiết kiệm 15%% (%.0f ₫/tháng), " +
                        "nên giảm %s %.0f%% từ %.0f ₫ xuống %.0f ₫. " +
                        "Loại: %s",
                        targetSavings, category.toLowerCase(), reductionPercent,
                        predicted, suggested, type);
            } else if (percentageOfIncome > 30 && isEssential) {
                // Danh mục thiết yếu chiếm quá cao (>30% thu nhập)
                double targetAmount = baseIncome * 0.25; // Mục tiêu 25% thu nhập
                suggested = Math.min(suggested, targetAmount);
                reason = String.format("⚠️ %s chiếm %.1f%% thu nhập (quá cao! nên dưới 30%%). " +
                        "Tìm cách tối ưu chi phí xuống %.0f ₫ để cân bằng ngân sách.",
                        category, percentageOfIncome, suggested);
            } else if (percentageOfIncome > 15 && !isEssential) {
                // Danh mục linh hoạt chiếm quá cao (>15% thu nhập)
                double targetAmount = baseIncome * 0.10; // Mục tiêu 10% thu nhập
                suggested = Math.min(suggested, targetAmount);
                reason = String.format("📉 %s (chi tiêu linh hoạt) chiếm %.1f%% thu nhập - quá cao! " +
                        "Nên giảm xuống %.0f ₫ (khoảng 10%% thu nhập) để tăng tiết kiệm.",
                        category, percentageOfIncome, suggested);
            } else if (percentageOfTotal > 40) {
                // Danh mục chiếm quá lớn trong tổng chi tiêu
                suggested = predicted * 0.80;
                reason = String.format("⚠️ %s chiếm %.1f%% tổng chi tiêu (quá mất cân đối!). " +
                        "Nên giảm xuống %.0f ₫ để phân bổ hợp lý hơn.",
                        category, percentageOfTotal, suggested);
            } else if (currentAmount > predicted * 1.3) {
                // Tháng này chi nhiều hơn bình thường
                suggested = Math.min(suggested, (predicted + currentAmount) / 2);
                reason = String.format("🔄 Tháng này %s chi %.0f ₫ (cao hơn thường lệ %.0f%%). " +
                        "Tháng sau điều chỉnh về %.0f ₫ để ổn định tài chính.",
                        category, currentAmount, ((currentAmount/predicted - 1) * 100), suggested);
            } else if (isEssential && percentageOfIncome < 5) {
                // Danh mục thiết yếu chi quá ít (có thể thiếu hụt)
                reason = String.format("📈 %s (thiết yếu) chỉ chiếm %.1f%% thu nhập. " +
                        "Ngân sách %.0f ₫/tháng là hợp lý, nhưng nếu cần thì tăng lên để đảm bảo chất lượng sống.",
                        category, percentageOfIncome, suggested);
            } else {
                // Mức chi tiêu hợp lý
                reason = String.format("✅ %s: %.0f ₫/tháng (%.1f%% thu nhập) - mức hợp lý! " +
                        "Duy trì để đảm bảo cân bằng tài chính. Loại: %s",
                        category, suggested, percentageOfIncome, type);
            }

            suggestions.add(com.expense.ai.dto.StatisticsResponse.BudgetSuggestion.builder()
                    .category(category)
                    .suggestedAmount(suggested)
                    .currentAverage(predicted)
                    .reason(reason)
                    .priority(type) // Sử dụng type (THIẾT YẾU/LINH HOẠT) làm priority
                    .build());
        });

        // Tính tổng chi tiêu được gợi ý
        double totalSuggested = suggestions.stream()
                .mapToDouble(com.expense.ai.dto.StatisticsResponse.BudgetSuggestion::getSuggestedAmount)
                .sum();
        double actualSavings = baseIncome - totalSuggested;
        double savingsRate = baseIncome > 0 ? (actualSavings / baseIncome) * 100 : 0;

        // Gợi ý TIẾT KIỆM - Mục tiêu chính: 10-20% thu nhập
        String savingsReason;
        if (savingsRate >= 20) {
            savingsReason = String.format("🌟 XUẤT SẮC! Tiết kiệm %.1f%% thu nhập (%.0f ₫/tháng = %.0f ₫/năm).\\n" +
                    "📊 Phân bổ ngân sách lý tưởng:\\n" +
                    "   • %.0f%% tiết kiệm (%.0f ₫) ✅\\n" +
                    "   • %.0f%% chi tiêu thiết yếu (%.0f ₫)\\n" +
                    "   • %.0f%% chi tiêu linh hoạt (%.0f ₫)\\n" +
                    "💡 Hướng dẫn sử dụng tiết kiệm:\\n" +
                    "   1. Quỹ khẩn cấp: 3-6 tháng chi phí (ưu tiên số 1)\\n" +
                    "   2. Mục tiêu ngắn hạn: Du lịch, mua sắm lớn\\n" +
                    "   3. Đầu tư dài hạn: Quỹ hưu trí, bất động sản",
                    savingsRate, actualSavings, actualSavings * 12,
                    savingsRate, actualSavings,
                    (totalEssentialPredicted * essentialAdjustment / baseIncome) * 100, totalEssentialPredicted * essentialAdjustment,
                    (totalFlexiblePredicted * flexibleAdjustment / baseIncome) * 100, totalFlexiblePredicted * flexibleAdjustment);
        } else if (savingsRate >= 15) {
            savingsReason = String.format("👍 TỐT! Tiết kiệm %.1f%% thu nhập (%.0f ₫/tháng = %.0f ₫/năm).\\n" +
                    "📊 Phân bổ ngân sách hiện tại:\\n" +
                    "   • %.0f%% tiết kiệm (%.0f ₫) ✅\\n" +
                    "   • %.0f%% chi tiêu (%.0f ₫)\\n" +
                    "🎯 Cải thiện thêm 5%% để đạt mức xuất sắc (20%%):\\n" +
                    "   → Giảm %.0f ₫/tháng chi tiêu linh hoạt (giải trí, mua sắm)\\n" +
                    "   → Tăng thu nhập thêm %.0f ₫/tháng\\n" +
                    "💰 Ưu tiên: Xây dựng quỹ khẩn cấp 3-6 tháng chi phí",
                    savingsRate, actualSavings, actualSavings * 12,
                    savingsRate, actualSavings,
                    100 - savingsRate, totalSuggested,
                    baseIncome * 0.05, baseIncome * 0.05);
        } else if (savingsRate >= 10) {
            savingsReason = String.format("⚡ CHẤP NHẬN ĐƯỢC. Tiết kiệm %.1f%% (%.0f ₫/tháng = %.0f ₫/năm).\\n" +
                    "⚠️ Mức này thấp hơn khuyến nghị (15-20%%)\\n" +
                    "📊 Cần cải thiện %.0f%% (%.0f ₫/tháng) để đạt mục tiêu:\\n" +
                    "   • Cắt giảm chi tiêu linh hoạt: %.0f ₫\\n" +
                    "   • Tối ưu chi tiêu thiết yếu: %.0f ₫\\n" +
                    "   • Hoặc tăng thu nhập: %.0f ₫\\n" +
                    "🎯 Kế hoạch hành động:\\n" +
                    "   1. Xem lại danh mục GIẢI TRÍ, MUA SẮM\\n" +
                    "   2. So sánh giá và tìm khuyến mãi cho chi phí thiết yếu\\n" +
                    "   3. Tìm nguồn thu nhập phụ",
                    savingsRate, actualSavings, actualSavings * 12,
                    15 - savingsRate, baseIncome * 0.05,
                    baseIncome * 0.03, baseIncome * 0.02, baseIncome * 0.05);
        } else if (savingsRate >= 5) {
            savingsReason = String.format("⚠️ CẢNH BÁO! Chỉ tiết kiệm %.1f%% (%.0f ₫/tháng) - QUÁ THẤP!\\n" +
                    "🚨 Thiếu %.0f%% (%.0f ₫/tháng) để đạt mục tiêu tối thiểu 15%%\\n" +
                    "📋 KẾ HOẠCH CẤP BÁCH:\\n" +
                    "   1. ❌ CẮT GIẢM CHI TIÊU LINH HOẠT:\\n" +
                    "      • Giải trí: Giảm 50%% (%.0f ₫)\\n" +
                    "      • Mua sắm: Giảm 70%% (%.0f ₫)\\n" +
                    "   2. 🔍 TỐI ƯU CHI TIÊU THIẾT YẾU:\\n" +
                    "      • Ăn uống: Nấu ăn tại nhà thay vì ăn ngoài\\n" +
                    "      • Giao thông: Dùng xe công cộng/xe đạp\\n" +
                    "   3. 💼 TĂNG THU NHẬP:\\n" +
                    "      • Làm thêm giờ, freelance, kinh doanh online\\n" +
                    "⏰ Hành động NGAY để tránh khủng hoảng tài chính!",
                    savingsRate, actualSavings,
                    15 - savingsRate, baseIncome * (0.15 - savingsRate/100),
                    totalFlexiblePredicted * 0.5, totalFlexiblePredicted * 0.7);
        } else if (savingsRate > 0) {
            savingsReason = String.format("🚨 NGUY HIỂM! Chỉ tiết kiệm %.1f%% (%.0f ₫) - KHÔNG BỀN VỮNG!\\n" +
                    "❌ Chi tiêu: %.0f ₫ (%.0f%% thu nhập) - QUÁ CAO!\\n" +
                    "⚠️ Đang ở bờ vực khủng hoảng tài chính!\\n\\n" +
                    "🆘 HÀNH ĐỘNG KHẨN CẤP:\\n" +
                    "   1. ❌ DỪNG TẤT CẢ CHI TIÊU KHÔNG CẦN THIẾT\\n" +
                    "      • Giải trí: 0 ₫\\n" +
                    "      • Mua sắm: 0 ₫\\n" +
                    "      • Ăn ngoài: 0 ₫\\n" +
                    "   2. 🔍 CHỈ CHI CHO NHU CẦU THIẾT YẾU\\n" +
                    "      • Ăn uống tối giản: %.0f ₫\\n" +
                    "      • Nhà ở: %.0f ₫\\n" +
                    "      • Giao thông: %.0f ₫\\n" +
                    "   3. 💼 TÌM THÊM THU NHẬP NGAY LẬP TỨC\\n" +
                    "   4. 📞 Tư vấn chuyên gia tài chính\\n\\n" +
                    "⏰ Không hành động = Nợ nần, khủng hoảng!",
                    savingsRate, actualSavings,
                    totalSuggested, (totalSuggested/baseIncome)*100,
                    baseIncome * 0.20, baseIncome * 0.25, baseIncome * 0.10);
        } else {
            savingsReason = String.format("❌❌❌ KHẨN CẤP! CHI TIÊU VƯỢT THU NHẬP %.0f ₫!\\n" +
                    "💣 Đang sống vượt quá khả năng %.0f%% - NGUY HIỂM CỰC ĐỘ!\\n" +
                    "📊 Thu nhập: %.0f ₫ | Chi tiêu: %.0f ₫\\n\\n" +
                    "🆘 KẾ HOẠCH CỨU VÃN NGAY HÔM NAY:\\n\\n" +
                    "   1. ⛔ DỪNG TẤT CẢ CHI TIÊU KHÔNG THIẾT YẾU\\n" +
                    "      • Hủy tất cả đăng ký dịch vụ không cần\\n" +
                    "      • Không mua sắm, giải trí, ăn ngoài\\n\\n" +
                    "   2. 💰 GIẢM CHI TIÊU CƠ BẢN XUỐNG MỨC TỐI THIỂU\\n" +
                    "      • Ăn uống: %.0f ₫ (tối giản)\\n" +
                    "      • Nhà ở: Xem xét giảm chi phí\\n" +
                    "      • Giao thông: Xe công cộng/đi bộ\\n\\n" +
                    "   3. 🚀 TĂNG THU NHẬP KHẨN CẤP\\n" +
                    "      • Làm thêm, bán đồ không dùng\\n" +
                    "      • Tìm công việc lương cao hơn\\n\\n" +
                    "   4. 📞 GỌI TƯ VẤN TÀI CHÍNH CHUYÊN NGHIỆP\\n\\n" +
                    "⚠️ TRÁNH VAY NỢ! Sẽ làm tình hình tồi tệ hơn!",
                    Math.abs(actualSavings),
                    (Math.abs(actualSavings)/baseIncome)*100,
                    baseIncome, totalSuggested,
                    baseIncome * 0.15);
        }

        suggestions.add(0, com.expense.ai.dto.StatisticsResponse.BudgetSuggestion.builder()
                .category("💰 TIẾT KIỆM")
                .suggestedAmount(actualSavings > 0 ? actualSavings : 0)
                .currentAverage(baseIncome - totalPredicted)
                .reason(savingsReason)
                .priority("CAO NHẤT")
                .build());

        // Sắp xếp: Tiết kiệm đầu tiên, sau đó theo ưu tiên
        suggestions.sort((a, b) -> {
            if (a.getCategory().equals("💰 TIẾT KIỆM")) return -1;
            if (b.getCategory().equals("💰 TIẾT KIỆM")) return 1;

            Map<String, Integer> priorityOrder = Map.of(
                    "CAO NHẤT", 0,
                    "CAO", 1,
                    "TRUNG BÌNH", 2,
                    "THẤP", 3
            );
            int comparison = priorityOrder.getOrDefault(a.getPriority(), 2)
                    .compareTo(priorityOrder.getOrDefault(b.getPriority(), 2));
            if (comparison != 0) return comparison;
            return b.getSuggestedAmount().compareTo(a.getSuggestedAmount());
        });

        return suggestions;
    }

    private List<String> generateStatisticsInsights(
            List<com.expense.ai.dto.StatisticsRequest.MonthlyData> monthlyData,
            double trend, double savingsPotential) {

        List<String> insights = new ArrayList<>();

        if (trend > 0.1) {
            insights.add(String.format("📈 Chi tiêu của bạn đang tăng %.1f%% so với trung bình", trend * 100));
        } else if (trend < -0.1) {
            insights.add(String.format("📉 Tuyệt vời! Chi tiêu giảm %.1f%% so với trung bình", Math.abs(trend) * 100));
        } else {
            insights.add("📊 Chi tiêu của bạn khá ổn định");
        }

        if (savingsPotential > 0) {
            insights.add(String.format("💰 Dự kiến tháng tới bạn có thể tiết kiệm %.0f VND", savingsPotential));
        } else {
            insights.add("⚠️ Dự kiến tháng tới chi tiêu sẽ vượt thu nhập");
        }

        // Calculate saving rate
        double avgIncome = monthlyData.stream()
                .mapToDouble(m -> m.getTotalIncome() != null ? m.getTotalIncome() : 0.0)
                .average().orElse(0.0);
        double avgExpense = monthlyData.stream()
                .mapToDouble(m -> m.getTotalExpense() != null ? m.getTotalExpense() : 0.0)
                .average().orElse(0.0);
        double savingRate = avgIncome > 0 ? ((avgIncome - avgExpense) / avgIncome) * 100 : 0.0;

        if (savingRate >= 20) {
            insights.add(String.format("✅ Tỷ lệ tiết kiệm %.1f%% - Xuất sắc!", savingRate));
        } else if (savingRate >= 10) {
            insights.add(String.format("👍 Tỷ lệ tiết kiệm %.1f%% - Khá tốt, cố gắng đạt 20%%!", savingRate));
        } else {
            insights.add(String.format("⚡ Tỷ lệ tiết kiệm %.1f%% - Cần cải thiện!", savingRate));
        }

        return insights;
    }

    private List<String> generateStatisticsWarnings(
            double trend, double savingsPotential, double predictedExpense, double avgExpense) {

        List<String> warnings = new ArrayList<>();

        if (trend > 0.2) {
            warnings.add("⚠️ CHI TIÊU TĂNG ĐỘT BIẾN: Chi tiêu tăng hơn 20% so với trung bình!");
        }

        if (savingsPotential < 0) {
            warnings.add("🚨 CẢNH BÁO: Dự kiến tháng tới sẽ chi nhiều hơn thu nhập!");
        }

        if (predictedExpense > avgExpense * 1.3) {
            warnings.add("⚡ Chi tiêu dự đoán cao hơn 30% so với trung bình - cần kiểm soát!");
        }

        return warnings;
    }

    /**
     * CẢI TIẾN: Tạo insights dựa trên THÁNG GẦN NHẤT - TIẾNG VIỆT
     */
    private List<String> generateStatisticsInsightsFromLatest(
            List<com.expense.ai.dto.StatisticsRequest.MonthlyData> monthlyData,
            double latestIncome, double latestExpense,
            double avgIncome, double avgExpense,
            double trend, double savingsPotential) {

        List<String> insights = new ArrayList<>();

        // So sánh tháng gần nhất với trung bình
        double incomeChange = avgIncome > 0 ? ((latestIncome - avgIncome) / avgIncome) * 100 : 0;
        double expenseChange = avgExpense > 0 ? ((latestExpense - avgExpense) / avgExpense) * 100 : 0;

        // Insight về thu nhập
        if (incomeChange > 10) {
            insights.add(String.format("💰 Tuyệt vời! Thu nhập tháng này tăng %.1f%% so với trung bình (%.0f ₫ → %.0f ₫)",
                    incomeChange, avgIncome, latestIncome));
        } else if (incomeChange < -10) {
            insights.add(String.format("📉 Lưu ý: Thu nhập tháng này giảm %.1f%% so với trung bình (%.0f ₫ → %.0f ₫)",
                    Math.abs(incomeChange), avgIncome, latestIncome));
        } else {
            insights.add(String.format("💼 Thu nhập tháng này ổn định ở mức %.0f ₫", latestIncome));
        }

        // Insight về chi tiêu
        if (expenseChange > 15) {
            insights.add(String.format("⚠️ Chi tiêu tháng này tăng %.1f%% so với trung bình (%.0f ₫ → %.0f ₫). Hãy kiểm soát!",
                    expenseChange, avgExpense, latestExpense));
        } else if (expenseChange < -15) {
            insights.add(String.format("🎉 Xuất sắc! Chi tiêu giảm %.1f%% so với trung bình (%.0f ₫ → %.0f ₫)",
                    Math.abs(expenseChange), avgExpense, latestExpense));
        } else {
            insights.add(String.format("📊 Chi tiêu tháng này khá ổn định ở mức %.0f ₫", latestExpense));
        }

        // Tỷ lệ tiết kiệm tháng này
        double currentSavingRate = latestIncome > 0 ? ((latestIncome - latestExpense) / latestIncome) * 100 : 0;

        if (currentSavingRate >= 25) {
            insights.add(String.format("🌟 Xuất sắc! Tháng này tiết kiệm %.1f%% thu nhập (%.0f ₫). Bạn đang làm rất tốt!",
                    currentSavingRate, latestIncome - latestExpense));
        } else if (currentSavingRate >= 20) {
            insights.add(String.format("✅ Tốt lắm! Tháng này tiết kiệm %.1f%% (%.0f ₫). Duy trì hoặc cải thiện thêm!",
                    currentSavingRate, latestIncome - latestExpense));
        } else if (currentSavingRate >= 15) {
            insights.add(String.format("👍 Khá tốt! Tháng này tiết kiệm %.1f%% (%.0f ₫). Cố gắng đạt 20%%!",
                    currentSavingRate, latestIncome - latestExpense));
        } else if (currentSavingRate >= 10) {
            insights.add(String.format("⚡ Tháng này tiết kiệm %.1f%% (%.0f ₫). Cần cải thiện lên 15-20%%!",
                    currentSavingRate, latestIncome - latestExpense));
        } else if (currentSavingRate >= 5) {
            insights.add(String.format("⚠️ Tháng này chỉ tiết kiệm %.1f%% (%.0f ₫). Quá thấp! Cần hành động ngay!",
                    currentSavingRate, latestIncome - latestExpense));
        } else if (currentSavingRate > 0) {
            insights.add(String.format("🚨 Nguy cơ! Chỉ tiết kiệm %.1f%% (%.0f ₫). Tình hình tài chính đáng lo ngại!",
                    currentSavingRate, latestIncome - latestExpense));
        } else {
            insights.add(String.format("❌ KHẨN CẤP! Chi tiêu vượt thu nhập %.0f ₫. Cần cắt giảm ngay!",
                    Math.abs(latestIncome - latestExpense)));
        }

        // Dự đoán tháng tới
        if (savingsPotential > 0) {
            insights.add(String.format("🔮 Dự đoán tháng tới: Có thể tiết kiệm %.0f ₫ nếu duy trì xu hướng hiện tại",
                    savingsPotential));
        } else {
            insights.add(String.format("⚠️ Dự đoán tháng tới: Có nguy cơ chi vượt thu %.0f ₫. Cần lập kế hoạch!",
                    Math.abs(savingsPotential)));
        }

        return insights;
    }

    /**
     * CẢI TIẾN: Tạo cảnh báo dựa trên THÁNG GẦN NHẤT - TIẾNG VIỆT
     */
    private List<String> generateStatisticsWarningsFromLatest(
            double trend, double savingsPotential, double latestExpense,
            double avgExpense, double latestIncome) {

        List<String> warnings = new ArrayList<>();

        // Cảnh báo chi tiêu tăng đột biến
        if (latestExpense > avgExpense * 1.3) {
            warnings.add(String.format("🚨 CHI TIÊU TẠI MÚC NGUY HIỂM! Tháng này chi %.0f ₫, cao hơn 30%% so với mức trung bình (%.0f ₫). " +
                    "Cần kiểm tra và cắt giảm ngay lập tức!", latestExpense, avgExpense));
        } else if (latestExpense > avgExpense * 1.2) {
            warnings.add(String.format("⚠️ CHI TIÊU TĂNG CAO: Tháng này chi %.0f ₫, tăng hơn 20%% so với trung bình (%.0f ₫). " +
                    "Hãy xem xét lại các khoản chi!", latestExpense, avgExpense));
        }

        // Cảnh báo xu hướng tăng
        if (trend > 0.15) {
            warnings.add(String.format("📈 XU HƯỚNG TĂNG ĐỘT BIẾN: Chi tiêu đang tăng %.1f%% theo xu hướng. " +
                    "Nếu không kiểm soát, tháng tới sẽ càng tăng cao!", trend * 100));
        }

        // Cảnh báo tiết kiệm thấp
        double currentSavingRate = latestIncome > 0 ? ((latestIncome - latestExpense) / latestIncome) * 100 : 0;
        if (currentSavingRate < 5 && currentSavingRate > 0) {
            warnings.add(String.format("💸 TỶ LỆ TIẾT KIỆM NGUY HIỂM THẤP: Chỉ %.1f%% (%.0f ₫). " +
                    "Bạn không có lưới an toàn tài chính! Cần hành động khẩn cấp!",
                    currentSavingRate, latestIncome - latestExpense));
        } else if (currentSavingRate <= 0) {
            warnings.add(String.format("❌ TÌNH TRẠNG KHẨN CẤP: Chi tiêu vượt thu nhập %.0f ₫! " +
                    "Bạn đang vay nợ hoặc dùng tiền tiết kiệm. Phải dừng ngay! " +
                    "Cắt giảm mọi chi phí không cần thiết, tìm nguồn thu thêm!",
                    Math.abs(latestIncome - latestExpense)));
        }

        // Cảnh báo dự đoán
        if (savingsPotential < 0) {
            warnings.add(String.format("🔴 DỰ ĐOÁN XẤU: Tháng tới có nguy cơ chi vượt thu %.0f ₫ nếu giữ nguyên thói quen. " +
                    "Hãy lập ngân sách chặt chẽ ngay từ bây giờ!",
                    Math.abs(savingsPotential)));
        } else if (savingsPotential < latestIncome * 0.05) {
            warnings.add(String.format("⚠️ TIẾT KIỆM RẤT THẤP: Dự đoán tháng tới chỉ tiết kiệm %.0f ₫ (dưới 5%% thu nhập). " +
                    "Cần tăng gấp đôi hoặc gấp ba để đảm bảo tương lai!",
                    savingsPotential));
        }

        // Cảnh báo đặc biệt cho từng tình huống
        if (latestExpense > latestIncome * 0.95 && latestExpense <= latestIncome) {
            warnings.add("💡 BIÊN AN TOÀN MỎNG MANH: Chi tiêu đã chiếm hơn 95% thu nhập. " +
                    "Một chi phí bất ngờ nhỏ cũng sẽ khiến bạn vượt ngân sách. Hãy cẩn thận!");
        }

        // Nếu không có cảnh báo nào, thêm lời khuyên tích cực
        if (warnings.isEmpty() && currentSavingRate >= 15) {
            warnings.add("✅ Tốt lắm! Không có cảnh báo đáng lo ngại. Tiếp tục duy trì thói quen tài chính tốt này!");
        }

        return warnings;
    }

    /**
     * TÍNH NĂNG MỚI 1: Tự động nhận diện danh mục từ mô tả giao dịch
     */
    public String categorizeTransaction(String description) {
        log.info("Categorizing transaction: {}", description);
        return geminiService.categorizeTransaction(description);
    }

    /**
     * TÍNH NĂNG MỚI 2: Phân tích bất thường chi tiêu
     */
    public com.expense.ai.dto.AnomalyAnalysis detectSpendingAnomalies(Long userId, Map<String, Object> request) {
        log.info("Detecting spending anomalies for user: {}", userId);

        @SuppressWarnings("unchecked")
        Map<String, Object> currentMonthRaw = (Map<String, Object>) request.get("currentMonth");
        @SuppressWarnings("unchecked")
        Map<String, Object> previousMonthRaw = (Map<String, Object>) request.get("previousMonth");

        // Convert to Double maps
        Map<String, Double> currentMonth = new HashMap<>();
        Map<String, Double> previousMonth = new HashMap<>();

        for (Map.Entry<String, Object> entry : currentMonthRaw.entrySet()) {
            currentMonth.put(entry.getKey(), convertToDouble(entry.getValue()));
        }
        for (Map.Entry<String, Object> entry : previousMonthRaw.entrySet()) {
            previousMonth.put(entry.getKey(), convertToDouble(entry.getValue()));
        }

        List<com.expense.ai.dto.AnomalyAnalysis.SpendingAnomaly> anomalies = new ArrayList<>();
        int anomalyCount = 0;
        String overallSeverity = "LOW";

        // So sánh từng danh mục
        for (Map.Entry<String, Double> entry : currentMonth.entrySet()) {
            String category = entry.getKey();
            double current = entry.getValue();
            double previous = previousMonth.getOrDefault(category, 0.0);

            if (previous == 0) continue; // Bỏ qua danh mục mới

            double changePercentage = ((current - previous) / previous) * 100;

            // Phát hiện bất thường (thay đổi > 30%)
            if (Math.abs(changePercentage) > 30) {
                String severity;
                String description;
                String possibleCause;
                String recommendation;

                if (Math.abs(changePercentage) > 100) {
                    severity = "CRITICAL";
                    overallSeverity = "CRITICAL";
                } else if (Math.abs(changePercentage) > 70) {
                    severity = "HIGH";
                    if (!overallSeverity.equals("CRITICAL")) overallSeverity = "HIGH";
                } else if (Math.abs(changePercentage) > 50) {
                    severity = "MEDIUM";
                    if (overallSeverity.equals("LOW")) overallSeverity = "MEDIUM";
                } else {
                    severity = "LOW";
                }

                if (changePercentage > 0) {
                    description = String.format("⚠️ %s tăng %.0f%% (từ %.0f ₫ lên %.0f ₫)",
                            category, changePercentage, previous, current);
                    possibleCause = "Chi tiêu bất thường, mua sắm lớn, hoặc thay đổi thói quen";
                    recommendation = String.format("Xem xét giảm %s xuống mức %.0f ₫ trong tháng tới",
                            category.toLowerCase(), (current + previous) / 2);
                } else {
                    description = String.format("✅ %s giảm %.0f%% (từ %.0f ₫ xuống %.0f ₫)",
                            category, Math.abs(changePercentage), previous, current);
                    possibleCause = "Tiết kiệm thành công hoặc giảm nhu cầu";
                    recommendation = "Duy trì mức chi tiêu này để tối ưu ngân sách";
                }

                anomalies.add(com.expense.ai.dto.AnomalyAnalysis.SpendingAnomaly.builder()
                        .category(category)
                        .currentAmount(current)
                        .previousAmount(previous)
                        .changePercentage(changePercentage)
                        .severity(severity)
                        .description(description)
                        .possibleCause(possibleCause)
                        .recommendation(recommendation)
                        .build());

                if (changePercentage > 0) anomalyCount++;
            }
        }

        // Tạo tóm tắt
        String summary = anomalyCount > 0 ?
                String.format("Phát hiện %d bất thường chi tiêu. Mức độ tổng thể: %s", anomalyCount, overallSeverity) :
                "Không phát hiện bất thường đáng kể. Chi tiêu ổn định so với tháng trước.";

        // Gợi ý
        List<String> recommendations = new ArrayList<>();
        if (anomalyCount > 0) {
            recommendations.add("📊 Xem xét chi tiết các giao dịch trong danh mục tăng bất thường");
            recommendations.add("💡 Lập kế hoạch chi tiêu rõ ràng cho tháng tới");
            recommendations.add("🎯 Đặt giới hạn chi tiêu cho từng danh mục");
        } else {
            recommendations.add("✅ Tiếp tục duy trì thói quen chi tiêu tốt");
            recommendations.add("💰 Tăng tỷ lệ tiết kiệm nếu có thể");
        }

        return com.expense.ai.dto.AnomalyAnalysis.builder()
                .hasAnomalies(anomalyCount > 0)
                .anomalyCount(anomalyCount)
                .overallSeverity(overallSeverity)
                .anomalies(anomalies)
                .summary(summary)
                .recommendations(recommendations)
                .build();
    }

    /**
     * TÍNH NĂNG MỚI 3: Gợi ý tối ưu chi tiêu thông minh
     */
    public com.expense.ai.dto.OptimizationSuggestions generateOptimizationSuggestions(Long userId, Map<String, Object> request) {
        log.info("Generating optimization suggestions for user: {}", userId);

        double totalIncome = convertToDouble(request.getOrDefault("totalIncome", 0.0));
        @SuppressWarnings("unchecked")
        Map<String, Object> categorySpendingRaw = (Map<String, Object>) request.get("categorySpending");

        // Convert to Double map
        Map<String, Double> categorySpending = new HashMap<>();
        for (Map.Entry<String, Object> entry : categorySpendingRaw.entrySet()) {
            categorySpending.put(entry.getKey(), convertToDouble(entry.getValue()));
        }

        double totalSpending = categorySpending.values().stream().mapToDouble(d -> d).sum();
        List<com.expense.ai.dto.OptimizationSuggestions.CategoryOptimization> optimizations = new ArrayList<>();
        double totalSavingsPotential = 0.0;

        // Phân loại danh mục
        Map<String, String> categoryType = Map.of(
                "ĂN UỐNG", "FLEXIBLE",
                "NHÀ Ở", "ESSENTIAL",
                "GIAO THÔNG", "ESSENTIAL",
                "MUA SẮM", "FLEXIBLE",
                "GIẢI TRÍ", "FLEXIBLE",
                "Y TẾ", "ESSENTIAL",
                "GIÁO DỤC", "ESSENTIAL",
                "KHÁC", "FLEXIBLE"
        );

        for (Map.Entry<String, Double> entry : categorySpending.entrySet()) {
            String category = entry.getKey();
            double current = entry.getValue();
            double percentOfIncome = (current / totalIncome) * 100;
            String type = categoryType.getOrDefault(category, "FLEXIBLE");

            double optimized = current;
            double savings = 0.0;
            String priority = "LOW";
            String difficulty = "EASY";
            String description = "";
            List<String> actionSteps = new ArrayList<>();
            String benefits = "";

            // Logic tối ưu dựa trên loại và % thu nhập
            if (type.equals("FLEXIBLE")) {
                if (percentOfIncome > 15) {
                    // Giảm 30% cho danh mục linh hoạt quá cao
                    optimized = current * 0.70;
                    savings = current - optimized;
                    priority = "HIGH";
                    difficulty = "MODERATE";
                    description = String.format("%s chiếm %.1f%% thu nhập (khuyến nghị < 15%%). " +
                            "Giảm %.0f%% xuống %.0f ₫ để cân bằng ngân sách.",
                            category, percentOfIncome, 30.0, optimized);
                    actionSteps = List.of(
                            "Lập danh sách ưu tiên cho " + category.toLowerCase(),
                            "Tìm các phương án tiết kiệm (khuyến mãi, thay thế rẻ hơn)",
                            "Đặt giới hạn chi tiêu hàng tuần"
                    );
                    benefits = String.format("Tiết kiệm %.0f ₫/tháng = %.0f ₫/năm", savings, savings * 12);
                } else if (percentOfIncome > 10) {
                    // Giảm 15% cho danh mục trung bình
                    optimized = current * 0.85;
                    savings = current - optimized;
                    priority = "MEDIUM";
                    difficulty = "EASY";
                    description = String.format("%s có thể tối ưu thêm %.0f%% để tiết kiệm %.0f ₫/tháng.",
                            category, 15.0, savings);
                    actionSteps = List.of(
                            "So sánh giá trước khi mua",
                            "Tìm các ưu đãi, giảm giá",
                            "Giảm tần suất chi tiêu"
                    );
                    benefits = String.format("Tiết kiệm %.0f ₫/tháng", savings);
                }
            } else if (type.equals("ESSENTIAL")) {
                if (percentOfIncome > 30) {
                    // Chi tiêu thiết yếu quá cao
                    optimized = current * 0.90; // Chỉ giảm 10%
                    savings = current - optimized;
                    priority = "MEDIUM";
                    difficulty = "HARD";
                    description = String.format("%s chiếm %.1f%% thu nhập (quá cao!). " +
                            "Tìm cách tối ưu chi phí mà không ảnh hưởng chất lượng sống.",
                            category, percentOfIncome);
                    actionSteps = List.of(
                            "So sánh nhà cung cấp để tìm giá tốt hơn",
                            "Xem xét các gói tiết kiệm dài hạn",
                            "Tối ưu hóa sử dụng để giảm lãng phí"
                    );
                    benefits = String.format("Giảm gánh nặng tài chính %.0f ₫/tháng", savings);
                }
            }

            if (savings > 0) {
                optimizations.add(com.expense.ai.dto.OptimizationSuggestions.CategoryOptimization.builder()
                        .category(category)
                        .currentSpending(current)
                        .optimizedSpending(optimized)
                        .savingsPotential(savings)
                        .reductionPercentage(((current - optimized) / current) * 100)
                        .priority(priority)
                        .difficulty(difficulty)
                        .description(description)
                        .actionSteps(actionSteps)
                        .benefits(benefits)
                        .build());

                totalSavingsPotential += savings;
            }
        }

        // Sắp xếp theo priority và savings
        optimizations.sort((a, b) -> {
            int priorityCompare = comparePriority(b.getPriority(), a.getPriority());
            return priorityCompare != 0 ? priorityCompare :
                    Double.compare(b.getSavingsPotential(), a.getSavingsPotential());
        });

        double savingsPercentage = (totalSavingsPotential / totalSpending) * 100;

        String summary = String.format(
                "💰 Tiềm năng tiết kiệm: %.0f ₫/tháng (%.1f%% chi tiêu hiện tại)\n" +
                        "📊 Tương đương: %.0f ₫/năm\n" +
                        "🎯 Nếu thực hiện đầy đủ, tỷ lệ tiết kiệm có thể tăng lên %.1f%%",
                totalSavingsPotential, savingsPercentage,
                totalSavingsPotential * 12,
                ((totalIncome - (totalSpending - totalSavingsPotential)) / totalIncome) * 100
        );

        List<String> priorityActions = optimizations.stream()
                .filter(o -> o.getPriority().equals("HIGH"))
                .map(o -> "⭐ " + o.getDescription())
                .limit(3)
                .toList();

        List<String> longTermSuggestions = List.of(
                "🏦 Mở tài khoản tiết kiệm tự động chuyển tiền mỗi tháng",
                "📈 Đầu tư số tiền tiết kiệm được vào quỹ sinh lời",
                "📝 Review và điều chỉnh ngân sách mỗi quý",
                "🎯 Đặt mục tiêu tài chính cụ thể (mua nhà, du lịch, hưu trí)"
        );

        return com.expense.ai.dto.OptimizationSuggestions.builder()
                .totalSavingsPotential(totalSavingsPotential)
                .savingsPercentage(savingsPercentage)
                .summary(summary)
                .categoryOptimizations(optimizations)
                .priorityActions(priorityActions.isEmpty() ?
                        List.of("✅ Chi tiêu đã được tối ưu tốt!") : priorityActions)
                .longTermSuggestions(longTermSuggestions)
                .build();
    }

    private int comparePriority(String p1, String p2) {
        Map<String, Integer> priorityOrder = Map.of("HIGH", 3, "MEDIUM", 2, "LOW", 1);
        return priorityOrder.getOrDefault(p1, 0) - priorityOrder.getOrDefault(p2, 0);
    }

    /**
     * Phân tích tính khả thi của mục tiêu tiết kiệm
     * Dựa trên thu nhập, chi tiêu cố định (recurring), và các mục tiêu hiện có
     */
    public Map<String, Object> analyzeSavingsGoalFeasibility(Long userId, Map<String, Object> request) {
        log.info("Analyzing savings goal feasibility for user: {}", userId);

        // Thu thập dữ liệu cần thiết cho phân tích
        Map<String, Object> goalData = new HashMap<>();

        // Thông tin mục tiêu
        goalData.put("goalName", request.get("goalName"));
        goalData.put("targetAmount", convertToDouble(request.get("targetAmount")));
        goalData.put("targetDate", request.get("targetDate"));
        goalData.put("daysRemaining", request.get("daysRemaining"));

        // Thu nhập và chi tiêu định kỳ
        goalData.put("recurringIncome", request.get("recurringIncome"));
        goalData.put("recurringExpenses", request.get("recurringExpenses"));

        // Các mục tiêu hiện có
        goalData.put("existingGoals", request.get("existingGoals"));

        // CẢI TIẾN: Tính toán tổng thu nhập và chi tiêu thực tế
        // Luôn ưu tiên sử dụng actual monthly income/expense nếu frontend gửi (bao gồm cả thu nhập bất ngờ)
        // Chỉ fallback về recurring nếu frontend KHÔNG GỬI actual (null/undefined)

        // Kiểm tra xem frontend có gửi actualMonthlyIncome không (khác null)
        boolean hasActualIncome = request.containsKey("actualMonthlyIncome") && request.get("actualMonthlyIncome") != null;
        boolean hasActualExpense = request.containsKey("actualMonthlyExpense") && request.get("actualMonthlyExpense") != null;

        double actualMonthlyIncome = hasActualIncome ? convertToDouble(request.get("actualMonthlyIncome")) : -1;
        double actualMonthlyExpense = hasActualExpense ? convertToDouble(request.get("actualMonthlyExpense")) : -1;

        @SuppressWarnings("unchecked")
        double recurringMonthlyIncome = calculateTotalMonthly(
                (List<Map<String, Object>>) request.get("recurringIncome"));
        @SuppressWarnings("unchecked")
        double recurringMonthlyExpenses = calculateTotalMonthly(
                (List<Map<String, Object>>) request.get("recurringExpenses"));

        // LUÔN ưu tiên actual nếu frontend gửi (kể cả khi = 0, vì 0 là hợp lệ - chưa có thu nhập)
        // Chỉ dùng recurring khi frontend không gửi actual
        double totalMonthlyIncome = hasActualIncome ? actualMonthlyIncome : recurringMonthlyIncome;
        double totalMonthlyExpenses = hasActualExpense ? actualMonthlyExpense : recurringMonthlyExpenses;

        log.info("=== DỮ LIỆU TÀI CHÍNH NHẬN TỪ FRONTEND ===");
        log.info("hasActualIncome: {}, actualMonthlyIncome: {}", hasActualIncome, actualMonthlyIncome);
        log.info("hasActualExpense: {}, actualMonthlyExpense: {}", hasActualExpense, actualMonthlyExpense);
        log.info("recurringMonthlyIncome: {}, recurringMonthlyExpenses: {}", recurringMonthlyIncome, recurringMonthlyExpenses);
        log.info(">>> FINAL: totalMonthlyIncome: {}, totalMonthlyExpenses: {}", totalMonthlyIncome, totalMonthlyExpenses);

        // Thêm thông tin actual vào goalData để Gemini phân tích chính xác hơn
        goalData.put("actualMonthlyIncome", actualMonthlyIncome);
        goalData.put("actualMonthlyExpense", actualMonthlyExpense);
        goalData.put("totalMonthlyIncome", totalMonthlyIncome);
        goalData.put("totalMonthlyExpenses", totalMonthlyExpenses);

        // Gọi Gemini để phân tích
        String aiAnalysis = geminiService.analyzeSavingsGoal(goalData);

        // Xác định mức độ khả thi dựa trên phân tích AI
        // QUAN TRỌNG: Phải kiểm tra "KHÔNG KHẢ THI" TRƯỚC "KHẢ THI"
        // vì "KHÔNG KHẢ THI" có chứa cả "KHẢ THI"
        String feasibility = "NEUTRAL";
        String aiUpper = aiAnalysis.toUpperCase();

        if (aiUpper.contains("KHÔNG KHẢ THI") || aiUpper.contains("KHONG KHA THI")) {
            feasibility = "NOT_FEASIBLE";
        } else if (aiUpper.contains("CẦN LƯU Ý") || aiUpper.contains("CAN LUU Y") || aiUpper.contains("THÁCH THỨC") || aiUpper.contains("THACH THUC")) {
            feasibility = "CHALLENGING";
        } else if (aiUpper.contains("KHẢ THI") || aiUpper.contains("KHA THI")) {
            feasibility = "FEASIBLE";
        }

        // Tính tổng các mục tiêu hiện có
        @SuppressWarnings("unchecked")
        double monthlyExistingGoals = calculateTotalExistingGoals(
                (List<Map<String, Object>>) request.get("existingGoals"));

        double monthlySurplus = totalMonthlyIncome - totalMonthlyExpenses - monthlyExistingGoals;

        Map<String, Object> result = new HashMap<>();
        result.put("aiAnalysis", aiAnalysis);
        result.put("feasibility", feasibility);
        result.put("monthlyIncome", totalMonthlyIncome);
        result.put("monthlyExpenses", totalMonthlyExpenses);
        result.put("monthlyExistingGoals", monthlyExistingGoals);
        result.put("monthlySurplus", monthlySurplus);

        return result;
    }

    /**
     * Tính tổng số tiền hàng tháng từ recurring transactions
     */
    private double calculateTotalMonthly(List<Map<String, Object>> recurring) {
        if (recurring == null || recurring.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (Map<String, Object> item : recurring) {
            double amount = convertToDouble(item.get("amount"));
            String pattern = (String) item.getOrDefault("recurrencePattern", "MONTHLY");

            // Convert về monthly amount
            switch (pattern) {
                case "DAILY":
                    total += amount * 30;
                    break;
                case "WEEKLY":
                    total += amount * 4;
                    break;
                case "MONTHLY":
                    total += amount;
                    break;
                case "YEARLY":
                    total += amount / 12;
                    break;
                default:
                    total += amount; // Default to monthly
            }
        }

        return total;
    }

    /**
     * Tính tổng số tiền tiết kiệm hàng tháng từ các mục tiêu hiện có
     */
    private double calculateTotalExistingGoals(List<Map<String, Object>> goals) {
        if (goals == null || goals.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (Map<String, Object> goal : goals) {
            total += convertToDouble(goal.get("monthlyRequired"));
        }

        return total;
    }

    /**
     * Gợi ý ngân sách mới sử dụng Gemini AI
     */
    public String suggestBudget(Long userId, Map<String, Object> budgetData) {
        log.info("AI suggesting budget for user: {}", userId);
        log.debug("Budget data: {}", budgetData);

        try {
            String suggestion = geminiService.suggestBudget(budgetData);
            log.info("Budget suggestion generated successfully for user: {}", userId);
            return suggestion;
        } catch (Exception e) {
            log.error("Error generating budget suggestion for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate budget suggestion: " + e.getMessage());
        }
    }
}
