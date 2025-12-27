package com.QhomeBase.assetmaintenanceservice.dto.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceTicketType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceTicketRequest {

    @NotBlank(message = "Ticket code is required")
    @Size(max = 64, message = "Ticket code must not exceed 64 characters")
    private String code;

    @NotBlank(message = "Ticket name is required")
    @Size(max = 255, message = "Ticket name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Ticket type is required")
    private ServiceTicketType ticketType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Duration hours must be positive")
    private BigDecimal durationHours;

    @NotNull(message = "Ticket price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    @Min(value = 1, message = "Max people must be at least 1")
    private Integer maxPeople;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Boolean isActive;

    private Integer sortOrder;
}

