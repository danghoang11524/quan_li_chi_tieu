package com.expense.insight.service;

import com.expense.insight.dto.WeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url:http://api.weatherapi.com/v1}")
    private String apiUrl;

    private final WebClient webClient;

    public WeatherService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public WeatherResponse getCurrentWeather(String location) {
        try {
            String url = String.format("%s/current.json?key=%s&q=%s&aqi=no",
                apiUrl, apiKey, location);

            log.info("Fetching weather for location: {}", location);

            return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .block();
        } catch (Exception e) {
            log.error("Error fetching weather data: {}", e.getMessage());
            return null;
        }
    }

    public String getWeatherBasedSpendingAdvice(String location) {
        WeatherResponse weather = getCurrentWeather(location);

        if (weather == null) {
            return "Không thể lấy thông tin thời tiết. Hãy cẩn thận với chi tiêu của bạn!";
        }

        StringBuilder advice = new StringBuilder();
        advice.append("🌤️ Thời tiết hôm nay tại ").append(weather.getLocation().getName()).append(": ");
        advice.append(weather.getCurrent().getCondition().getText());
        advice.append(" (").append(weather.getCurrent().getTempC()).append("°C)\n\n");

        String condition = weather.getCurrent().getCondition().getText().toLowerCase();

        if (condition.contains("rain") || condition.contains("mưa")) {
            advice.append("☔ Trời mưa - Cân nhắc:\n");
            advice.append("• Có thể cần chi thêm cho taxi/grab thay vì đi xe máy\n");
            advice.append("• Đừng quên mang ô để tiết kiệm tiền đi lại\n");
            advice.append("• Tránh shopping online khi trời mưa (dễ mua sắm cảm xúc!)\n");
        } else if (condition.contains("sunny") || condition.contains("nắng")) {
            advice.append("☀️ Trời nắng đẹp - Lưu ý:\n");
            advice.append("• Thời tiết thuận lợi để đi bộ/xe đạp, tiết kiệm tiền xăng\n");
            advice.append("• Cẩn thận chi tiêu cho đồ uống giải nhiệt\n");
            advice.append("• Thời điểm tốt để làm việc ngoài trời miễn phí (công viên, cafe vỉa hè)\n");
        } else if (condition.contains("cloud") || condition.contains("mây")) {
            advice.append("☁️ Trời nhiều mây - Gợi ý:\n");
            advice.append("• Thời tiết dễ chịu, phù hợp đi bộ tiết kiệm\n");
            advice.append("• Tránh chi tiêu không cần thiết cho các hoạt động trong nhà\n");
        }

        if (weather.getCurrent().getTempC() > 32) {
            advice.append("\n🔥 Nhiệt độ cao - Cảnh báo:\n");
            advice.append("• Hạn chế mua đồ ăn nhanh, tự nấu ăn tiết kiệm hơn\n");
            advice.append("• Tiết kiệm điện bằng cách sử dụng quạt thay vì điều hòa nếu có thể\n");
        } else if (weather.getCurrent().getTempC() < 20) {
            advice.append("\n🧊 Trời mát - Lưu ý:\n");
            advice.append("• Thời điểm tốt để đi chợ, mua sắm thông minh\n");
            advice.append("• Tiết kiệm điện khi không cần điều hòa\n");
        }

        return advice.toString();
    }
}
