package com.QhomeBase.datadocsservice.model.imports;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@Data
@Builder
public class ContractImportRow {

    private int rowNumber;
    private UUID unitId;
    private String contractNumber;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal purchasePrice;
    private String paymentMethod;
    private String paymentTerms;
    private LocalDate purchaseDate;
    private String notes;
    private String status;
}








