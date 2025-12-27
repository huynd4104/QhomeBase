package com.QhomeBase.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateGroupRequest {
    
    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 255, message = "Group name must be between 1 and 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private UUID buildingId;
    
    private List<UUID> memberIds; // Initial members to add (excluding creator)
}

