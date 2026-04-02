package com.expense.insight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.expense.insight", "com.expense.common"})
public class InsightServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsightServiceApplication.class, args);
    }
}
