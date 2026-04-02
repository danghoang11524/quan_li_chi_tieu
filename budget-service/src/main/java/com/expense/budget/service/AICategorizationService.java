package com.expense.budget.service;

import com.expense.budget.dto.AICategorySuggestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class AICategorizationService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent}")
    private String geminiApiUrl;

    public AICategorySuggestion categorizeExpense(String description) {
        if (description == null || description.trim().isEmpty()) {
            return createDefaultSuggestion();
        }

        // Check if API key is configured
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            log.warn("Gemini API key not configured in budget-service, using fallback categorization");
            return createDefaultSuggestion();
        }

        try {
            log.info("Calling Gemini API for expense categorization: '{}'", description);

            // Build the prompt for Gemini
            String prompt = buildCategorizationPrompt(description);

            // Call Gemini API
            String requestBody = buildGeminiRequest(prompt);

            String response = webClientBuilder.build()
                    .post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Received response from Gemini API");

            // Parse response
            return parseGeminiResponse(response);

        } catch (Exception e) {
            log.error("Error calling Gemini AI for categorization: {}", e.getMessage(), e);
            return createDefaultSuggestion();
        }
    }

    private String buildCategorizationPrompt(String description) {
        return String.format(
                "Phân tích mô tả chi tiêu này và phân loại nó.\n\n" +
                "Mô tả: \"%s\"\n\n" +
                "Các danh mục có sẵn:\n" +
                "- DINING: Đồ ăn, nhà hàng, quán cà phê, tạp hóa\n" +
                "- TRANSPORTATION: Taxi, xe buýt, tàu hỏa, xăng dầu, bãi đỗ xe, bảo dưỡng xe\n" +
                "- ENTERTAINMENT: Phim ảnh, hòa nhạc, trò chơi, sở thích\n" +
                "- SHOPPING: Quần áo, điện tử, đồ gia dụng\n" +
                "- BILLS: Tiền thuê nhà, tiện ích, điện thoại, internet, đăng ký dịch vụ\n" +
                "- HEALTHCARE: Bác sĩ, thuốc men, bệnh viện, bảo hiểm\n" +
                "- EDUCATION: Học phí, sách vở, khóa học, đào tạo\n" +
                "- OTHER: Bất kỳ chi phí nào không phù hợp với các danh mục trên\n\n" +
                "Loại chi tiêu:\n" +
                "- RECURRING: Chi phí thường xuyên, có thể dự đoán (tiền thuê nhà, đăng ký dịch vụ, hóa đơn hàng tháng)\n" +
                "- INCIDENTAL: Chi phí không thường xuyên, bất ngờ (mua sắm, ăn ngoài, chi phí ngoài kế hoạch)\n\n" +
                "Vui lòng trả lời ở định dạng JSON với các trường sau:\n" +
                "{\n" +
                "  \"suggestedCategory\": \"<TÊN_DANH_MỤC>\",\n" +
                "  \"suggestedType\": \"RECURRING\" hoặc \"INCIDENTAL\",\n" +
                "  \"confidence\": \"HIGH\" hoặc \"MEDIUM\" hoặc \"LOW\",\n" +
                "  \"reasoning\": \"Giải thích ngắn gọn bằng tiếng Việt\"\n" +
                "}\n\n" +
                "Chỉ trả lời bằng JSON hợp lệ, không thêm văn bản khác.",
                description
        );
    }

    private String buildGeminiRequest(String prompt) {
        return String.format(
                "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}",
                escapeJson(prompt)
        );
    }

    private AICategorySuggestion parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();

                    // Extract JSON from response text
                    text = text.trim();
                    if (text.startsWith("```json")) {
                        text = text.substring(7);
                    }
                    if (text.startsWith("```")) {
                        text = text.substring(3);
                    }
                    if (text.endsWith("```")) {
                        text = text.substring(0, text.length() - 3);
                    }
                    text = text.trim();

                    // Parse the AI's JSON response
                    JsonNode suggestion = objectMapper.readTree(text);

                    return new AICategorySuggestion(
                            suggestion.path("suggestedCategory").asText("OTHER"),
                            suggestion.path("suggestedType").asText("INCIDENTAL"),
                            suggestion.path("confidence").asText("MEDIUM"),
                            suggestion.path("reasoning").asText("AI analysis completed")
                    );
                }
            }

            return createDefaultSuggestion();

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            return createDefaultSuggestion();
        }
    }

    private AICategorySuggestion createDefaultSuggestion() {
        return new AICategorySuggestion(
                "OTHER",
                "INCIDENTAL",
                "LOW",
                "Unable to determine category automatically"
        );
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
