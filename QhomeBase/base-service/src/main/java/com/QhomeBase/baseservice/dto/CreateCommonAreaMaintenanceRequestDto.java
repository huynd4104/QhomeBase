package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public record CreateCommonAreaMaintenanceRequestDto(
        UUID buildingId, // Optional - for building-specific requests

        @NotBlank(message = "Area type is required")
        String areaType, // e.g., "Hành lang", "Thang máy", "Đèn khu vực chung", "Bãi xe", etc.

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be less than 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        List<String> attachments,

        @NotBlank(message = "Location is required")
        String location, // Specific location description (e.g., "Tầng 5, hành lang A")

        String contactName,

        String contactPhone,

        String note
) {
    public CreateCommonAreaMaintenanceRequestDto {
        attachments = attachments == null
                ? List.of()
                : attachments.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .limit(5) // Allow more attachments for common area requests
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
