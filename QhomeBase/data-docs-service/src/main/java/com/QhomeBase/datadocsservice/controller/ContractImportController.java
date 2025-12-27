package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.dto.imports.ContractImportResponse;
import com.QhomeBase.datadocsservice.service.ContractImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Tag(name = "Contract Import", description = "Import contracts from Excel")
public class ContractImportController {

    private final ContractImportService contractImportService;

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(summary = "Import contracts from Excel (.xlsx)")
    public ResponseEntity<ContractImportResponse> importContracts(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "createdBy", required = false) UUID createdBy
    ) {
        if (createdBy == null) {
            createdBy = UUID.randomUUID();
        }
        ContractImportResponse response = contractImportService.importContracts(file, createdBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download contract import template (.xlsx)")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = contractImportService.generateTemplateWorkbook();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract_import_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}


