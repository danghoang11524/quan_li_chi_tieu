package com.expense.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service để tích hợp với Google Gemini API
 * Sử dụng REST API trực tiếp để gọi Gemini
 */
@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gọi Gemini API để chat
     * @param userMessage Tin nhắn từ người dùng
     * @param context Ngữ cảnh tài chính của người dùng
     * @return Phản hồi từ Gemini
     */
    public String chat(String userMessage, String context) {
        // Kiểm tra API key
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured, using fallback response");
            return getFallbackResponse(userMessage);
        }

        try {
            // Tạo system prompt linh hoạt - có thể trả lời mọi câu hỏi
            String systemPrompt = "Bạn là trợ lý AI thông minh của Google Gemini, có khả năng trả lời mọi câu hỏi. " +
                    "Bạn có kiến thức rộng về mọi lĩnh vực: khoa học, công nghệ, lịch sử, văn hóa, nghệ thuật, giải trí, thể thao, và nhiều hơn nữa. " +
                    "Luôn trả lời bằng tiếng Việt (trừ khi được yêu cầu ngôn ngữ khác), chính xác, dễ hiểu và hữu ích. " +
                    "Sử dụng emoji phù hợp để câu trả lời sinh động hơn.\n\n" +
                    "Lưu ý: Nếu câu hỏi liên quan đến tài chính cá nhân, hãy đưa ra lời khuyên thực tế và cá nhân hóa.\n" +
                    (context != null && !context.isEmpty() ? "Ngữ cảnh: " + context + "\n\n" : "");

            String fullPrompt = systemPrompt + "Câu hỏi: " + userMessage;

            // Tạo request body
            Map<String, Object> requestBody = new HashMap<>();

            Map<String, Object> part = new HashMap<>();
            part.put("text", fullPrompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            requestBody.put("contents", List.of(content));

            // Cấu hình generation
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.put("generationConfig", generationConfig);

            // Cấu hình safety settings
            List<Map<String, Object>> safetySettings = List.of(
                Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE")
            );
            requestBody.put("safetySettings", safetySettings);

            // Tạo headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Gọi API với API key
            String urlWithKey = apiUrl + "?key=" + apiKey;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Calling Gemini API for user message: {}", userMessage);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(urlWithKey, request, Map.class);

            // Parse response
            if (response != null && response.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> candidate = candidates.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");
                    if (!parts.isEmpty()) {
                        String text = (String) parts.get(0).get("text");
                        log.info("Gemini API response received successfully");
                        return text;
                    }
                }
            }

            log.warn("Unexpected response format from Gemini API");
            return getFallbackResponse(userMessage);

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return getFallbackResponse(userMessage);
        }
    }

    /**
     * Tự động nhận diện danh mục chi tiêu từ mô tả bằng AI
     * @param description Mô tả giao dịch (ví dụ: "mua cà phê starbucks", "đổ xăng xe")
     * @return Tên danh mục (ĂN UỐNG, GIAO THÔNG, MUA SẮM, v.v.)
     */
    public String categorizeTransaction(String description) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured, using fallback categorization");
            return categorizeFallback(description);
        }

        // Retry với exponential backoff cho 503 errors
        int maxRetries = 3;
        int retryDelayMs = 500; // Bắt đầu với 0.5 giây (nhanh hơn vì categorization cần response nhanh)

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String prompt = "Bạn là chuyên gia phân loại chi tiêu. Nhiệm vụ: Phân loại giao dịch vào ĐÚNG 1 trong các danh mục sau:\n\n" +
                        "1. ĂN UỐNG - Ăn sáng, trưa, tối, café, trà sữa, nhà hàng, quán ăn, food, drink\n" +
                        "2. NHÀ Ở - Tiền nhà, tiền điện, nước, internet, sửa chữa nhà, nội thất\n" +
                        "3. GIAO THÔNG - Xăng xe, gửi xe, taxi, grab, xe buýt, MRT, vé máy bay, vé tàu\n" +
                        "4. MUA SẮM - Quần áo, giày dép, mỹ phẩm, điện tử, đồ dùng, shopping\n" +
                        "5. GIẢI TRÍ - Phim, game, du lịch, spa, massage, karaoke, bar, concert\n" +
                        "6. Y TẾ - Bệnh viện, phòng khám, thuốc, khám bệnh, nha khoa\n" +
                        "7. GIÁO DỤC - Học phí, sách vở, khóa học online, gia sư\n" +
                        "8. KHÁC - Tất cả các loại khác không thuộc 7 danh mục trên\n\n" +
                        "MÔ TẢ GIAO DỊCH: \"" + description + "\"\n\n" +
                        "CHỈ TRẢ VỀ TÊN DANH MỤC (VÍ DỤ: ĂN UỐNG), KHÔNG GIẢI THÍCH THÊM!";

                // Tạo request body
                Map<String, Object> requestBody = new HashMap<>();
                Map<String, Object> part = new HashMap<>();
                part.put("text", prompt);
                Map<String, Object> content = new HashMap<>();
                content.put("parts", List.of(part));
                requestBody.put("contents", List.of(content));

                // Cấu hình generation - ngắn gọn, chính xác
                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("temperature", 0.3); // Giảm nhiệt độ để chính xác hơn
                generationConfig.put("maxOutputTokens", 20); // Chỉ cần vài từ
                requestBody.put("generationConfig", generationConfig);

                // Gọi API
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String urlWithKey = apiUrl + "?key=" + apiKey;
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                log.info("Calling Gemini API for categorization (attempt {}/{}): '{}'", attempt, maxRetries, description);

                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(urlWithKey, request, Map.class);

                // Check for error in response
                if (response != null && response.containsKey("error")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> error = (Map<String, Object>) response.get("error");
                    int errorCode = error.containsKey("code") ? (int) error.get("code") : 0;
                    String errorMessage = error.containsKey("message") ? (String) error.get("message") : "";

                    // Nếu là 429 (Quota Exceeded), không retry - fallback ngay
                    if (errorCode == 429) {
                        log.warn("Gemini API quota exceeded (429) for categorization. Message: {}. Using fallback.", errorMessage);
                        log.info("ℹ️ Gemini Free Tier limit: 20 requests/day. Consider upgrading or waiting for quota reset.");
                        return categorizeFallback(description);
                    }

                    // Nếu là 503 (Service Unavailable) và còn lần retry, thử lại
                    if (errorCode == 503 && attempt < maxRetries) {
                        log.warn("Gemini API overloaded (503), retrying categorization in {}ms...", retryDelayMs);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff: 0.5s -> 1s -> 2s
                        continue;
                    }

                    log.error("Gemini API returned error for categorization: {}", error);
                    return categorizeFallback(description);
                }

                // Parse response
                if (response != null && response.containsKey("candidates")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> candidate = candidates.get(0);
                        if (candidate != null && candidate.containsKey("content")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                            if (contentResponse != null && contentResponse.containsKey("parts")) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");
                                if (parts != null && !parts.isEmpty()) {
                                    String category = ((String) parts.get(0).get("text")).trim().toUpperCase();
                                    // Validate category
                                    if (isValidCategory(category)) {
                                        log.info("AI categorized '{}' as '{}'", description, category);
                                        return category;
                                    }
                                }
                            }
                        }
                    }
                }

                log.warn("Could not categorize transaction, response format unexpected");

                // Nếu còn lần retry và format không đúng, thử lại
                if (attempt < maxRetries) {
                    log.info("Retrying categorization in {}ms...", retryDelayMs);
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2;
                    continue;
                }

                return categorizeFallback(description);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Categorization retry interrupted: {}", e.getMessage());
                return categorizeFallback(description);
            } catch (Exception e) {
                String errorMessage = e.getMessage();

                // Nếu là 429 (Quota Exceeded), không retry - fallback ngay
                if (errorMessage != null && errorMessage.contains("429")) {
                    log.warn("Gemini API quota exceeded (429 exception) for categorization. Using fallback.");
                    log.info("ℹ️ Gemini Free Tier limit: 20 requests/day. Consider upgrading or waiting for quota reset.");
                    return categorizeFallback(description);
                }

                // Nếu là HttpServerErrorException với 503 và còn lần retry, thử lại
                if (errorMessage != null && errorMessage.contains("503") && attempt < maxRetries) {
                    log.warn("Gemini API overloaded (503 exception), retrying categorization in {}ms...", retryDelayMs);
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                        continue;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Categorization retry interrupted: {}", ie.getMessage());
                        return categorizeFallback(description);
                    }
                }

                log.error("Error calling Gemini API for categorization: {}", e.getMessage());

                // Nếu còn lần retry, thử lại với các lỗi khác
                if (attempt < maxRetries) {
                    log.info("Retrying categorization in {}ms...", retryDelayMs);
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                        continue;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return categorizeFallback(description);
                    }
                }

                return categorizeFallback(description);
            }
        }

        // Nếu hết retry vẫn lỗi
        log.error("All retry attempts failed for categorization, using fallback");
        return categorizeFallback(description);
    }

    /**
     * Kiểm tra xem danh mục có hợp lệ không
     */
    private boolean isValidCategory(String category) {
        List<String> validCategories = List.of(
                "ĂN UỐNG", "NHÀ Ở", "GIAO THÔNG", "MUA SẮM",
                "GIẢI TRÍ", "Y TẾ", "GIÁO DỤC", "KHÁC"
        );
        return validCategories.stream().anyMatch(category::contains);
    }

    /**
     * Phân loại dự phòng khi không có AI
     */
    private String categorizeFallback(String description) {
        String desc = description.toLowerCase();

        if (desc.matches(".*(ăn|uống|cà phê|cafe|coffee|trà|quán|nhà hàng|cơm|phở|bún|bánh|food|drink).*")) {
            return "ĂN UỐNG";
        } else if (desc.matches(".*(nhà|điện|nước|gas|internet|wifi|thuê|rent).*")) {
            return "NHÀ Ở";
        } else if (desc.matches(".*(xăng|xe|taxi|grab|bus|tàu|máy bay|gửi xe|vé|transport).*")) {
            return "GIAO THÔNG";
        } else if (desc.matches(".*(quần|áo|giày|shop|mua|mall|siêu thị|market).*")) {
            return "MUA SẮM";
        } else if (desc.matches(".*(phim|game|du lịch|travel|spa|massage|karaoke).*")) {
            return "GIẢI TRÍ";
        } else if (desc.matches(".*(bệnh|thuốc|khám|doctor|hospital|clinic).*")) {
            return "Y TẾ";
        } else if (desc.matches(".*(học|sách|course|khóa học|gia sư).*")) {
            return "GIÁO DỤC";
        } else {
            return "KHÁC";
        }
    }

    /**
     * Phản hồi dự phòng khi không thể gọi Gemini API
     */
    private String getFallbackResponse(String userMessage) {
        String message = userMessage.toLowerCase();

        if (message.contains("xin chào") || message.contains("chào") || message.contains("hi") || message.contains("hello")) {
            return "Xin chào! 👋 Tôi là trợ lý tài chính AI của bạn. Tôi có thể giúp bạn:\n" +
                   "💰 Phân tích chi tiêu và thu nhập\n" +
                   "📊 Lập kế hoạch ngân sách\n" +
                   "🎯 Theo dõi mục tiêu tiết kiệm\n" +
                   "📈 Dự đoán xu hướng tài chính\n\n" +
                   "Bạn muốn tôi giúp gì?";
        } else if (message.contains("tiết kiệm") || message.contains("tiet kiem")) {
            return "💰 **Chiến lược tiết kiệm hiệu quả:**\n\n" +
                   "1️⃣ Quy tắc 50/30/20: 50% thiết yếu, 30% mong muốn, 20% tiết kiệm\n" +
                   "2️⃣ Tự động hóa việc tiết kiệm\n" +
                   "3️⃣ Xây dựng quỹ khẩn cấp 3-6 tháng\n" +
                   "4️⃣ Cắt giảm chi phí không cần thiết\n\n" +
                   "💡 Mẹo: Tiết kiệm ngay khi nhận lương!";
        } else if (message.contains("ngân sách") || message.contains("ngan sach")) {
            return "📊 **Cách lập ngân sách:**\n\n" +
                   "Bước 1: Tính tổng thu nhập\n" +
                   "Bước 2: Liệt kê chi phí cố định\n" +
                   "Bước 3: Ước tính chi phí biến động\n" +
                   "Bước 4: Đặt giới hạn cho từng danh mục\n" +
                   "Bước 5: Theo dõi và điều chỉnh hàng tháng\n\n" +
                   "✅ Sử dụng app này để tự động hóa!";
        } else {
            return "Tôi hiểu bạn muốn hỏi về \"" + userMessage + "\".\n\n" +
                   "Để trả lời chính xác hơn, tôi cần kết nối với Gemini AI. " +
                   "Vui lòng cấu hình GEMINI_API_KEY trong file .env hoặc biến môi trường.\n\n" +
                   "Bạn có thể lấy API key miễn phí tại: https://makersuite.google.com/app/apikey\n\n" +
                   "💡 Trong thời gian chờ, tôi vẫn có thể giúp bạn về:\n" +
                   "• Tiết kiệm\n" +
                   "• Ngân sách\n" +
                   "• Chi tiêu\n" +
                   "• Đầu tư";
        }
    }

    /**
     * Phân tích tính khả thi của mục tiêu tiết kiệm
     * @param goalData Thông tin mục tiêu và tài chính hiện tại
     * @return Phân tích chi tiết về tính khả thi
     */
    public String analyzeSavingsGoal(Map<String, Object> goalData) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured, using fallback analysis");
            return getFallbackSavingsAnalysis(goalData);
        }

        // Retry với exponential backoff cho 503 errors
        int maxRetries = 3;
        int retryDelayMs = 1000; // Bắt đầu với 1 giây

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Xây dựng prompt phân tích
                String prompt = buildSavingsAnalysisPrompt(goalData);
                log.info("=== PROMPT GỬI CHO GEMINI ===\n{}", prompt);

                // Tạo request body
                Map<String, Object> requestBody = new HashMap<>();
                Map<String, Object> part = new HashMap<>();
                part.put("text", prompt);
                Map<String, Object> content = new HashMap<>();
                content.put("parts", List.of(part));
                requestBody.put("contents", List.of(content));

                // Cấu hình generation - đủ cho thinking mode + câu trả lời chi tiết
                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("temperature", 0.5);
                generationConfig.put("maxOutputTokens", 2500); // Tăng lên để AI có thể trả lời chi tiết với gợi ý
                requestBody.put("generationConfig", generationConfig);

                // Gọi API
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String urlWithKey = apiUrl + "?key=" + apiKey;
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                log.info("Calling Gemini API for savings goal analysis (attempt {}/{})", attempt, maxRetries);

                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(urlWithKey, request, Map.class);

                // Log response for debugging
                log.debug("Gemini API response: {}", response);

                // Check for error in response
                if (response != null && response.containsKey("error")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> error = (Map<String, Object>) response.get("error");
                    int errorCode = error.containsKey("code") ? (int) error.get("code") : 0;
                    String errorMessage = error.containsKey("message") ? (String) error.get("message") : "";

                    // Nếu là 429 (Quota Exceeded), không retry - fallback ngay
                    if (errorCode == 429) {
                        log.warn("Gemini API quota exceeded (429) for savings analysis. Message: {}. Using fallback.", errorMessage);
                        log.info("ℹ️ Gemini Free Tier limit: 20 requests/day. Consider upgrading or waiting for quota reset.");
                        return getFallbackSavingsAnalysis(goalData);
                    }

                    // Nếu là 503 (Service Unavailable) và còn lần retry, thử lại
                    if (errorCode == 503 && attempt < maxRetries) {
                        log.warn("Gemini API overloaded (503), retrying in {}ms...", retryDelayMs);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff: 1s -> 2s -> 4s
                        continue;
                    }

                    log.error("Gemini API returned error: {}", error);
                    return getFallbackSavingsAnalysis(goalData);
                }

                // Parse response
                if (response != null && response.containsKey("candidates")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> candidate = candidates.get(0);
                        if (candidate != null && candidate.containsKey("content")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                            if (contentResponse != null && contentResponse.containsKey("parts")) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");
                                if (parts != null && !parts.isEmpty()) {
                                    String analysis = (String) parts.get(0).get("text");
                                    log.info("Gemini savings goal analysis completed successfully");
                                    return analysis;
                                }
                            }
                        }
                    }
                }

                log.warn("Unexpected response format from Gemini API: {}", response);

                // Nếu còn lần retry và format không đúng, thử lại
                if (attempt < maxRetries) {
                    log.info("Retrying in {}ms...", retryDelayMs);
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2;
                    continue;
                }

                return getFallbackSavingsAnalysis(goalData);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Retry interrupted: {}", e.getMessage());
                return getFallbackSavingsAnalysis(goalData);
            } catch (Exception e) {
                String errorMessage = e.getMessage();

                // Nếu là 429 (Quota Exceeded), không retry - fallback ngay
                if (errorMessage != null && errorMessage.contains("429")) {
                    log.warn("Gemini API quota exceeded (429 exception) for savings analysis. Using fallback.");
                    log.info("ℹ️ Gemini Free Tier limit: 20 requests/day. Consider upgrading or waiting for quota reset.");
                    return getFallbackSavingsAnalysis(goalData);
                }

                // Nếu là HttpServerErrorException với 503 và còn lần retry, thử lại
                if (errorMessage != null && errorMessage.contains("503") && attempt < maxRetries) {
                    log.warn("Gemini API overloaded (503 exception), retrying in {}ms...", retryDelayMs);
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                        continue;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted: {}", ie.getMessage());
                        return getFallbackSavingsAnalysis(goalData);
                    }
                }

                log.error("Error calling Gemini API for savings analysis: {}", e.getMessage(), e);

                // Nếu còn lần retry, thử lại với các lỗi khác
                if (attempt < maxRetries) {
                    log.info("Retrying in {}ms...", retryDelayMs);
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                        continue;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return getFallbackSavingsAnalysis(goalData);
                    }
                }

                return getFallbackSavingsAnalysis(goalData);
            }
        }

        // Nếu hết retry vẫn lỗi
        log.error("All retry attempts failed, using fallback analysis");
        return getFallbackSavingsAnalysis(goalData);
    }

    /**
     * Xây dựng prompt để phân tích mục tiêu tiết kiệm
     */
    private String buildSavingsAnalysisPrompt(Map<String, Object> goalData) {
        StringBuilder prompt = new StringBuilder();

        // CẢI TIẾN: Sử dụng totalMonthlyIncome/Expenses đã được tính từ AIAnalysisService
        // AIAnalysisService đã ưu tiên actual, chỉ fallback recurring khi không có actual
        double totalIncome = convertToDouble(goalData.get("totalMonthlyIncome"));
        double totalExpenses = convertToDouble(goalData.get("totalMonthlyExpenses"));

        // Không cần fallback ở đây nữa vì AIAnalysisService đã xử lý
        // totalIncome và totalExpenses có thể = 0 (hợp lệ - chưa có thu/chi trong tháng)

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> existingGoals = (List<Map<String, Object>>) goalData.get("existingGoals");
        double totalExistingGoals = calculateTotalExistingGoals(existingGoals);
        double monthlySurplus = totalIncome - totalExpenses - totalExistingGoals;

        double targetAmount = convertToDouble(goalData.get("targetAmount"));
        int daysRemaining = (int) goalData.getOrDefault("daysRemaining", 30);
        double monthsRemaining = daysRemaining / 30.0;
        double requiredMonthlySaving = monthsRemaining > 0 ? targetAmount / monthsRemaining : targetAmount;

        // Tính toán khả thi
        boolean isFeasible = monthlySurplus >= requiredMonthlySaving;

        // CẢI TIẾN: Hiển thị thông tin rõ ràng hơn
        // actualMonthlyIncome có thể = -1 nếu frontend không gửi, hoặc >= 0 nếu có gửi (kể cả = 0)
        double actualMonthlyIncome = convertToDouble(goalData.get("actualMonthlyIncome"));
        double actualMonthlyExpense = convertToDouble(goalData.get("actualMonthlyExpense"));

        prompt.append("Phân tích mục tiêu tiết kiệm:\n\n");
        prompt.append("• Mục tiêu: ").append(goalData.get("goalName")).append("\n");
        prompt.append("• Cần tiết kiệm: ").append(formatMoney(targetAmount)).append(" VND trong ").append(String.format("%.0f", monthsRemaining)).append(" tháng (").append(formatMoney(requiredMonthlySaving)).append("/tháng)\n");

        // Hiển thị thu nhập: nếu có actual (>= 0) thì dùng actual, không thì dùng recurring
        if (actualMonthlyIncome >= 0) {
            prompt.append("• Thu nhập thực tế tháng này: ").append(formatMoney(actualMonthlyIncome));
            if (actualMonthlyIncome == 0) {
                prompt.append(" (chưa có thu nhập trong tháng)");
            } else {
                prompt.append(" (bao gồm cả thu nhập bất ngờ)");
            }
            prompt.append("\n");
        } else {
            prompt.append("• Thu nhập định kỳ dự kiến: ").append(formatMoney(totalIncome)).append("\n");
        }

        // Hiển thị chi tiêu: nếu có actual (>= 0) thì dùng actual, không thì dùng recurring
        if (actualMonthlyExpense >= 0) {
            prompt.append("• Chi tiêu thực tế tháng này: ").append(formatMoney(actualMonthlyExpense));
            if (actualMonthlyExpense == 0) {
                prompt.append(" (chưa có chi tiêu trong tháng)");
            }
            prompt.append("\n");
        } else {
            prompt.append("• Chi tiêu định kỳ dự kiến: ").append(formatMoney(totalExpenses)).append("\n");
        }

        prompt.append("• Tiết kiệm cho mục tiêu khác: ").append(formatMoney(totalExistingGoals)).append("\n");
        prompt.append("• Còn dư: ").append(formatMoney(monthlySurplus)).append("\n\n");

        prompt.append("Viết phân tích CHI TIẾT bằng tiếng Việt (KHÔNG VIẾT tiêu đề như '**KHẢ THI**', '**KHÔNG KHẢ THI**', v.v., chỉ viết nội dung phân tích):\n\n");

        // Thêm hướng dẫn đặc biệt khi thu nhập = 0
        if (totalIncome == 0) {
            prompt.append("[Lưu ý: Thu nhập hiện tại là 0. Hãy khuyên người dùng cần thêm nguồn thu nhập hoặc ghi nhận thu nhập hàng tháng vào hệ thống để phân tích chính xác hơn. Sau đó phân tích khả năng đạt mục tiêu nếu có thu nhập.]");
        } else if (!isFeasible) {
            // Khi mục tiêu KHÔNG KHẢ THI, đưa ra gợi ý chi tiết
            prompt.append("[YÊU CẦU PHÂN TÍCH CHI TIẾT:\n");
            prompt.append("1. Giải thích ngắn gọn (2-3 câu) tại sao mục tiêu không khả thi dựa trên số liệu.\n");
            prompt.append("2. GỢI Ý TĂNG THU NHẬP (liệt kê 3-4 gợi ý cụ thể, ví dụ: làm thêm, freelance, đầu tư nhỏ, bán đồ không dùng, tăng ca, xin tăng lương...).\n");
            prompt.append("3. GỢI Ý GIẢM CHI TIÊU (liệt kê 3-4 gợi ý cụ thể, ví dụ: cắt giảm ăn uống ngoài, giảm mua sắm không cần thiết, tiết kiệm điện nước, dùng phương tiện công cộng, hủy các dịch vụ không cần thiết...).\n");
            prompt.append("4. Kết luận động viên (1 câu).\n");
            prompt.append("Định dạng: Mỗi phần trên một dòng riêng, rõ ràng, dễ đọc.]");
        } else if (monthlySurplus < requiredMonthlySaving * 1.2) {
            // Khi mục tiêu KHẢ THI nhưng CẦN LƯU Ý (dư ít)
            prompt.append("[YÊU CẦU PHÂN TÍCH:\n");
            prompt.append("1. Giải thích ngắn gọn (1-2 câu) tại sao mục tiêu khả thi nhưng cần chú ý.\n");
            prompt.append("2. GỢI Ý BỔ SUNG THU NHẬP (2-3 gợi ý để tăng thêm thu nhập dự phòng).\n");
            prompt.append("3. GỢI Ý TIẾT KIỆM (2-3 gợi ý để giảm chi tiêu không cần thiết).\n");
            prompt.append("4. Kết luận khuyến khích (1 câu).]");
        } else {
            // Khi mục tiêu hoàn toàn KHẢ THI
            prompt.append("[Giải thích ngắn gọn (2-3 câu) tại sao mục tiêu khả thi. Khuyến khích người dùng và gợi ý một số cách tối ưu hóa tiết kiệm.]");
        }

        return prompt.toString();
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
     * Convert object to double
     */
    private double convertToDouble(Object value) {
        if (value == null) return 0.0;
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
     * Format số tiền
     */
    private String formatMoney(Object amount) {
        if (amount == null) return "0";
        if (amount instanceof Number) {
            return String.format("%,.0f", ((Number) amount).doubleValue());
        }
        return amount.toString();
    }

    /**
     * Phân tích dự phòng khi không có AI
     */
    private String getFallbackSavingsAnalysis(Map<String, Object> goalData) {
        return "⚠️ **Chế độ phân tích cơ bản**\n\n" +
               "Để nhận phân tích chi tiết từ AI, vui lòng cấu hình Gemini API key.\n\n" +
               "**Phân tích sơ bộ:**\n" +
               "📊 Mục tiêu của bạn: " + goalData.get("goalName") + "\n" +
               "💰 Số tiền cần tiết kiệm: " + formatMoney(goalData.get("targetAmount")) + " VND\n" +
               "⏰ Thời gian: " + goalData.get("daysRemaining") + " ngày\n\n" +
               "💡 **Gợi ý chung:**\n" +
               "1. Tạo các khoản thu nhập cố định trong mục 'Giao dịch định kỳ'\n" +
               "2. Theo dõi chi tiêu hàng ngày để kiểm soát tốt hơn\n" +
               "3. Áp dụng quy tắc 50/30/20 cho ngân sách\n" +
               "4. Tự động chuyển tiền tiết kiệm ngay khi nhận lương\n\n" +
               "✅ Bạn có thể đạt được mục tiêu nếu kiên trì!";
    }

    /**
     * Gợi ý ngân sách mới dựa trên các ngân sách hiện có
     * @param budgetData Thông tin ngân sách mới và các ngân sách hiện có
     * @return Phân tích và gợi ý từ AI
     */
    public String suggestBudget(Map<String, Object> budgetData) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured, using fallback budget suggestion");
            return getFallbackBudgetSuggestion(budgetData);
        }

        // Retry với exponential backoff cho 503 errors
        int maxRetries = 3;
        int retryDelayMs = 1000; // Bắt đầu với 1 giây

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Tạo context từ existing budgets
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> existingBudgets = (List<Map<String, Object>>) budgetData.get("existingBudgets");

                StringBuilder budgetContext = new StringBuilder();
                if (existingBudgets != null && !existingBudgets.isEmpty()) {
                    budgetContext.append("**Ngân sách hiện tại của người dùng:**\n");
                    for (Map<String, Object> budget : existingBudgets) {
                        String category = (String) budget.get("category");
                        Object amountObj = budget.get("amount");
                        Object spentObj = budget.get("spentAmount");
                        String period = (String) budget.get("period");

                        Double amount = amountObj instanceof Integer ? ((Integer) amountObj).doubleValue() : (Double) amountObj;
                        Double spent = spentObj != null ? (spentObj instanceof Integer ? ((Integer) spentObj).doubleValue() : (Double) spentObj) : 0.0;

                        double percentage = amount > 0 ? (spent / amount * 100) : 0;

                        budgetContext.append(String.format("- %s: %s VND/%s (đã chi: %s VND = %.1f%%)\n",
                            category, formatMoney(amount), period, formatMoney(spent), percentage));
                    }
                } else {
                    budgetContext.append("Người dùng chưa có ngân sách nào.\n");
                }

                String newCategory = (String) budgetData.get("newCategory");
                Object newAmountObj = budgetData.get("newAmount");
                Double newAmount = newAmountObj instanceof Integer ? ((Integer) newAmountObj).doubleValue() : (Double) newAmountObj;
                String newPeriod = (String) budgetData.get("newPeriod");

                // Lấy thu nhập cố định
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recurringIncomes = (List<Map<String, Object>>) budgetData.get("recurringIncomes");
                StringBuilder incomeContext = new StringBuilder();
                double totalMonthlyIncome = 0.0;
                if (recurringIncomes != null && !recurringIncomes.isEmpty()) {
                    incomeContext.append("**Thu nhập cố định hàng tháng:**\n");
                    for (Map<String, Object> income : recurringIncomes) {
                        String desc = (String) income.get("description");
                        Object amountObj = income.get("amount");
                        String freq = (String) income.get("frequency");
                        Double amount = amountObj instanceof Integer ? ((Integer) amountObj).doubleValue() : (Double) amountObj;

                        // Quy đổi về tháng
                        double monthlyAmount = amount;
                        if ("WEEKLY".equals(freq)) monthlyAmount = amount * 4.33;
                        else if ("DAILY".equals(freq)) monthlyAmount = amount * 30;
                        else if ("YEARLY".equals(freq)) monthlyAmount = amount / 12;

                        totalMonthlyIncome += monthlyAmount;
                        incomeContext.append(String.format("- %s: %s VND/%s (≈ %s VND/tháng)\n",
                            desc, formatMoney(amount), freq, formatMoney(monthlyAmount)));
                    }
                    incomeContext.append(String.format("**Tổng thu nhập/tháng: %s VND**\n\n", formatMoney(totalMonthlyIncome)));
                } else {
                    incomeContext.append("Người dùng chưa có thông tin thu nhập cố định.\n\n");
                }

                // Lấy lịch sử chi tiêu
                @SuppressWarnings("unchecked")
                Map<String, Object> spendingHistory = (Map<String, Object>) budgetData.get("spendingHistory");
                StringBuilder historyContext = new StringBuilder();
                if (spendingHistory != null && !spendingHistory.isEmpty()) {
                    historyContext.append("**Lịch sử chi tiêu trong 30 ngày qua:**\n");
                    Object totalSpentObj = spendingHistory.get("totalSpent");
                    Double totalSpent = totalSpentObj instanceof Integer ? ((Integer) totalSpentObj).doubleValue() : (Double) totalSpentObj;
                    historyContext.append(String.format("- Tổng chi: %s VND\n", formatMoney(totalSpent)));

                    @SuppressWarnings("unchecked")
                    Map<String, Object> byCategory = (Map<String, Object>) spendingHistory.get("byCategory");
                    if (byCategory != null && !byCategory.isEmpty()) {
                        historyContext.append("- Chi tiêu theo danh mục:\n");
                        for (Map.Entry<String, Object> entry : byCategory.entrySet()) {
                            Object catAmountObj = entry.getValue();
                            Double catAmount = catAmountObj instanceof Integer ? ((Integer) catAmountObj).doubleValue() : (Double) catAmountObj;
                            historyContext.append(String.format("  + %s: %s VND\n", entry.getKey(), formatMoney(catAmount)));
                        }
                    }
                    historyContext.append("\n");
                }

                // Tính tổng ngân sách hiện tại (quy đổi về tháng)
                double totalExistingBudgets = 0.0;
                if (existingBudgets != null && !existingBudgets.isEmpty()) {
                    for (Map<String, Object> budget : existingBudgets) {
                        Object amountObj = budget.get("amount");
                        String period = (String) budget.get("period");
                        Double amount = amountObj instanceof Integer ? ((Integer) amountObj).doubleValue() : (Double) amountObj;

                        // Quy đổi về tháng
                        if ("DAILY".equals(period)) amount *= 30;
                        else if ("WEEKLY".equals(period)) amount *= 4.33;
                        else if ("YEARLY".equals(period)) amount /= 12;

                        totalExistingBudgets += amount;
                    }
                }

                String prompt = "Bạn là chuyên gia tài chính cá nhân. Người dùng muốn thêm ngân sách mới:\n\n" +
                        "**Ngân sách mới:**\n" +
                        "- Danh mục: " + newCategory + "\n" +
                        "- Số tiền: " + formatMoney(newAmount) + " VND\n" +
                        "- Chu kỳ: " + newPeriod + "\n\n" +
                        incomeContext.toString() +
                        budgetContext.toString() + "\n" +
                        historyContext.toString() +
                        "**Nhiệm vụ của bạn:**\n" +
                        "1. Phân tích xem ngân sách mới này có HỢP LÝ không dựa trên:\n" +
                        "   - Thu nhập cố định của người dùng (tổng: " + formatMoney(totalMonthlyIncome) + " VND/tháng)\n" +
                        "   - Tổng ngân sách hiện tại đã có (≈ " + formatMoney(totalExistingBudgets) + " VND/tháng)\n" +
                        "   - Lịch sử chi tiêu thực tế trong danh mục " + newCategory + "\n" +
                        "   - Tỷ lệ chi tiêu thực tế của các ngân sách hiện có\n" +
                        "   - Sự cân đối giữa thu nhập và tổng chi tiêu dự kiến\n\n" +
                        "2. Đưa ra GỢI Ý CỤ THỂ:\n" +
                        "   - NÊN tạo ngân sách này KHÔNG? (Trả lời rõ ràng: CÓ hoặc KHÔNG)\n" +
                        "   - Số tiền đề xuất có phù hợp không? Nếu không, đề xuất số tiền hợp lý dựa trên thu nhập và lịch sử chi tiêu\n" +
                        "   - Cảnh báo nếu có (ví dụ: vượt quá thu nhập, tổng ngân sách quá cao, không đủ tiền sau khi trừ các ngân sách khác, v.v.)\n" +
                        "   - 2-3 lời khuyên thực tế để quản lý ngân sách hiệu quả hơn\n\n" +
                        "**Yêu cầu định dạng:**\n" +
                        "Trả lời theo cấu trúc JSON sau (không thêm markdown ```json):\n" +
                        "{\n" +
                        "  \"shouldCreate\": true/false,\n" +
                        "  \"recommendation\": \"Gợi ý ngắn gọn 1-2 câu\",\n" +
                        "  \"reason\": \"Lý do chi tiết\",\n" +
                        "  \"suggestedAmount\": số tiền đề xuất (hoặc null nếu số tiền hiện tại OK),\n" +
                        "  \"warnings\": [\"cảnh báo 1\", \"cảnh báo 2\"],\n" +
                        "  \"tips\": [\"lời khuyên 1\", \"lời khuyên 2\", \"lời khuyên 3\"]\n" +
                        "}";

                // Tạo request body
                Map<String, Object> requestBody = new HashMap<>();

                Map<String, Object> part = new HashMap<>();
                part.put("text", prompt);

                Map<String, Object> content = new HashMap<>();
                content.put("parts", List.of(part));

                requestBody.put("contents", List.of(content));

                // Cấu hình generation
                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("temperature", 0.7);
                generationConfig.put("maxOutputTokens", 2048);
                requestBody.put("generationConfig", generationConfig);

                // Gọi API
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String urlWithKey = apiUrl + "?key=" + apiKey;
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                log.info("Calling Gemini API for budget suggestion (attempt {}/{})", attempt, maxRetries);

                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(urlWithKey, request, Map.class);

                // Check for error in response
                if (response != null && response.containsKey("error")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> error = (Map<String, Object>) response.get("error");
                    int errorCode = error.containsKey("code") ? (int) error.get("code") : 0;
                    String errorMessage = error.containsKey("message") ? (String) error.get("message") : "";

                    // Nếu là 429 (Quota Exceeded), không retry - fallback ngay
                    if (errorCode == 429) {
                        log.warn("Gemini API quota exceeded (429) for budget suggestion. Message: {}. Using fallback.", errorMessage);
                        return getFallbackBudgetSuggestion(budgetData);
                    }

                    // Nếu là 503 (Service Unavailable) và còn lần retry, thử lại
                    if (errorCode == 503 && attempt < maxRetries) {
                        log.warn("Gemini API overloaded (503), retrying in {}ms...", retryDelayMs);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff
                        continue;
                    }

                    log.error("Gemini API returned error: {}", error);
                    return getFallbackBudgetSuggestion(budgetData);
                }

                // Parse response
                if (response != null && response.containsKey("candidates")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> candidate = candidates.get(0);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                        if (contentResponse != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                String text = (String) parts.get(0).get("text");
                                log.info("Gemini API budget suggestion received successfully");
                                return text;
                            }
                        }
                    }
                }

                log.warn("Unexpected response format from Gemini API for budget suggestion");
                return getFallbackBudgetSuggestion(budgetData);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting to retry Gemini API call", e);
                return getFallbackBudgetSuggestion(budgetData);
            } catch (Exception e) {
                log.error("Error calling Gemini API for budget suggestion (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    return getFallbackBudgetSuggestion(budgetData);
                }
                // Retry for other errors
                try {
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return getFallbackBudgetSuggestion(budgetData);
                }
            }
        }

        return getFallbackBudgetSuggestion(budgetData);
    }

    /**
     * Gợi ý ngân sách dự phòng khi không có AI
     */
    private String getFallbackBudgetSuggestion(Map<String, Object> budgetData) {
        String newCategory = (String) budgetData.get("newCategory");
        Object newAmountObj = budgetData.get("newAmount");
        Double newAmount = null;

        // Add null-safety check for newAmountObj
        if (newAmountObj != null) {
            newAmount = newAmountObj instanceof Integer ? ((Integer) newAmountObj).doubleValue() : (Double) newAmountObj;
        }

        // Default category if not provided
        String category = (newCategory != null && !newCategory.isEmpty()) ? newCategory : "danh mục này";

        return "{\n" +
               "  \"shouldCreate\": true,\n" +
               "  \"recommendation\": \"Bạn có thể tạo ngân sách này. Để nhận phân tích chi tiết từ AI, vui lòng cấu hình Gemini API key.\",\n" +
               "  \"reason\": \"Chế độ phân tích cơ bản không thể đánh giá chi tiết mức độ hợp lý của ngân sách. Tuy nhiên, việc thiết lập ngân sách cho " + category + " là một bước tốt để quản lý tài chính.\",\n" +
               "  \"suggestedAmount\": null,\n" +
               "  \"warnings\": [\"Không có dữ liệu AI để phân tích chi tiết\", \"Vui lòng tự đánh giá mức độ phù hợp của ngân sách\"],\n" +
               "  \"tips\": [\"Theo dõi chi tiêu thường xuyên để điều chỉnh ngân sách phù hợp\", \"Áp dụng quy tắc 50/30/20: 50% nhu cầu thiết yếu, 30% mong muốn, 20% tiết kiệm\", \"Xem xét tổng thu nhập và các khoản chi tiêu khác trước khi thiết lập ngân sách mới\"]\n" +
               "}";
    }
}
