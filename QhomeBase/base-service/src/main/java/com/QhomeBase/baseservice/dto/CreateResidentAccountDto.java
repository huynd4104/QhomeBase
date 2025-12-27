package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.Size;

public record CreateResidentAccountDto(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,
        
        boolean autoGenerate
) {}


