package com.QhomeBase.assetmaintenanceservice.dto.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePricingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookingCatalogDto {

    private UUID serviceId;
    private String serviceCode;
    private String serviceName;
    private ServicePricingType pricingType;
    private List<ServiceComboDto> combos;
    private List<ServiceOptionDto> options;
    private List<ServiceTicketDto> tickets;
}



