package com.QhomeBase.baseservice.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {
    private LocalDate dueDate;
    private String currency;
    private String billToName;
    private String billToAddress;
    private String billToContact;
    private UUID payerUnitId;
    private UUID payerResidentId;
    private UUID cycleId;
    private String status; 
    private List<CreateInvoiceLineRequest> lines;
}

