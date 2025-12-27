package com.QhomeBase.iamservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableRoleDto {
    
    private String roleName;
    private String description;
    private List<String> permissions;
    private boolean isAssignable;
    private String category;
}








































