package com.QhomeBase.chatservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AddMembersRequest {
    
    @NotEmpty(message = "At least one member ID is required")
    private List<UUID> memberIds;
}

