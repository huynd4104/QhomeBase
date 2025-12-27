package com.QhomeBase.chatservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateGroupRequest {
    
    @Size(min = 1, max = 255, message = "Group name must be between 1 and 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private String avatarUrl;
}

