package com.expense.insight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {
    private Location location;
    private Current current;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private String name;
        private String region;
        private String country;
        @JsonProperty("localtime")
        private String localTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Current {
        @JsonProperty("temp_c")
        private Double tempC;

        @JsonProperty("temp_f")
        private Double tempF;

        @JsonProperty("is_day")
        private Integer isDay;

        private Condition condition;

        @JsonProperty("wind_kph")
        private Double windKph;

        @JsonProperty("humidity")
        private Integer humidity;

        @JsonProperty("feelslike_c")
        private Double feelsLikeC;

        @JsonProperty("uv")
        private Double uv;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        private String text;
        private String icon;
        private Integer code;
    }
}
