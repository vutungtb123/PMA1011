package com.example.school_library_system.dto;

import lombok.Data;

@Data
public class OtpVerifyDto {
    private String email;
    private String otpCode;
}
