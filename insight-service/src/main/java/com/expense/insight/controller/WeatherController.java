package com.expense.insight.controller;

import com.expense.insight.dto.WeatherResponse;
import com.expense.insight.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrentWeather(
            @RequestParam(defaultValue = "Hanoi") String location) {
        WeatherResponse weather = weatherService.getCurrentWeather(location);
        return ResponseEntity.ok(weather);
    }

    @GetMapping("/advice")
    public ResponseEntity<String> getWeatherBasedAdvice(
            @RequestParam(defaultValue = "Hanoi") String location) {
        String advice = weatherService.getWeatherBasedSpendingAdvice(location);
        return ResponseEntity.ok(advice);
    }
}
