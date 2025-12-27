package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.service.PdfFormFillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@Tag(name = "PDF Form", description = "Fill PDF templates (AcroForm)")
@RequiredArgsConstructor
public class PdfFormController {

    private final PdfFormFillService pdfFormFillService;

    @PostMapping(value = "/fill", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fill PDF template by field map and return PDF bytes")
    public ResponseEntity<byte[]> fill(@RequestParam(defaultValue = "templates/contract_template.pdf") String templatePath,
                                       @RequestParam(defaultValue = "filled.pdf") String filename,
                                       @RequestParam(defaultValue = "true") boolean flatten,
                                       @RequestBody Map<String, String> payload) {
        byte[] pdf = pdfFormFillService.fillTemplate(templatePath, payload, flatten);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}









