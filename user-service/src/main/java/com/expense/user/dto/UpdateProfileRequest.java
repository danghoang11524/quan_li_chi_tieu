package com.expense.user.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String fullName;

    private String phoneNumber;

    private String currency;

    private Double monthlyIncome;

    private String avatarUrl;
}
