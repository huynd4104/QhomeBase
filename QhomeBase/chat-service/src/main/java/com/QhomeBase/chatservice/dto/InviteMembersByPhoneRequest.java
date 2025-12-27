package com.QhomeBase.chatservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class InviteMembersByPhoneRequest {
    
    @NotEmpty(message = "At least one phone number is required")
    private List<String> phoneNumbers;
}

