package com.expense.savings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.expense.savings", "com.expense.common"})
public class SavingsGoalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SavingsGoalServiceApplication.class, args);
    }
}
