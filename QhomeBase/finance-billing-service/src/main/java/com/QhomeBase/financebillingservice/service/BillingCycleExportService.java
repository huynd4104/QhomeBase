package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.dto.BuildingInvoiceSummaryDto;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingCycleExportService {

    private final BillingCycleInvoiceService billingCycleInvoiceService;
    private final BaseServiceClient baseServiceClient;

    @Transactional(readOnly = true)
    public byte[] exportBillingCycleToExcel(UUID cycleId, String serviceCode, String month, UUID buildingId) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle dateStyle = createDateStyle(wb);

            List<BuildingInvoiceSummaryDto> summaries = billingCycleInvoiceService.summarizeByCycle(cycleId, serviceCode, month);
            
            if (buildingId != null) {
                summaries = summaries.stream()
                        .filter(s -> s.getBuildingId() != null && buildingId.equals(s.getBuildingId()))
                        .toList();
                
                if (summaries.isEmpty()) {
                    log.warn("No building summaries found for buildingId={}, trying to load invoices directly", buildingId);
                    List<InvoiceDto> invoices = billingCycleInvoiceService.getInvoicesByBuilding(
                            cycleId, buildingId, serviceCode, month);
                    
                    if (!invoices.isEmpty()) {
                        BuildingInvoiceSummaryDto directSummary = BuildingInvoiceSummaryDto.builder()
                                .buildingId(buildingId)
                                .build();
                        summaries = List.of(directSummary);
                    }
                }
            }

            Sheet summarySheet = wb.createSheet("Tổng hợp theo tòa");
            createSummarySheet(summarySheet, summaries, headerStyle, currencyStyle);
            autoSizeColumns(summarySheet);

            Map<UUID, String> unitNamesMap = new HashMap<>();
            
            for (BuildingInvoiceSummaryDto summary : summaries) {
                if (summary.getBuildingId() == null) continue;
                
                UUID currentBuildingId = summary.getBuildingId();
                List<InvoiceDto> invoices = billingCycleInvoiceService.getInvoicesByBuilding(
                        cycleId, currentBuildingId, serviceCode, month);
                
                for (InvoiceDto invoice : invoices) {
                    if (invoice.getPayerUnitId() != null && !unitNamesMap.containsKey(invoice.getPayerUnitId())) {
                        try {
                            BaseServiceClient.UnitInfo unit = baseServiceClient.getUnitById(invoice.getPayerUnitId());
                            if (unit != null) {
                                String displayName = (unit.getName() != null && !unit.getName().trim().isEmpty()) 
                                    ? unit.getName() 
                                    : (unit.getCode() != null && !unit.getCode().trim().isEmpty()) 
                                        ? unit.getCode() 
                                        : invoice.getPayerUnitId().toString();
                                if (unit.getFloor() != null) {
                                    displayName = "Tầng " + unit.getFloor() + " - " + displayName;
                                }
                                unitNamesMap.put(invoice.getPayerUnitId(), displayName);
                            } else {
                                unitNamesMap.put(invoice.getPayerUnitId(), invoice.getPayerUnitId().toString());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to load unit {}: {}", invoice.getPayerUnitId(), e.getMessage());
                            unitNamesMap.put(invoice.getPayerUnitId(), invoice.getPayerUnitId().toString());
                        }
                    }
                }
            }
            
            for (BuildingInvoiceSummaryDto summary : summaries) {
                if (summary.getBuildingId() == null) continue;
                
                UUID currentBuildingId = summary.getBuildingId();
                List<InvoiceDto> invoices = billingCycleInvoiceService.getInvoicesByBuilding(
                        cycleId, currentBuildingId, serviceCode, month);
                
                String sheetName = generateSheetName(summary);
                Sheet buildingSheet = wb.createSheet(sheetName);
                int rowNum = 0;
                
                createInvoiceHeader(buildingSheet, rowNum++, headerStyle);
                
                if (invoices.isEmpty()) {
                    log.debug("No invoices found for buildingId={} in cycleId={}", currentBuildingId, cycleId);
                    Row emptyRow = buildingSheet.createRow(rowNum++);
                    emptyRow.createCell(0).setCellValue("Không có hóa đơn");
                } else {
                    for (InvoiceDto invoice : invoices) {
                        createInvoiceRow(buildingSheet, rowNum++, invoice, summary, currencyStyle, dateStyle, unitNamesMap);
                    }
                }
                
                autoSizeColumns(buildingSheet);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Error exporting billing cycle to Excel", e);
            throw new IllegalStateException("Không thể xuất Excel billing cycle", e);
        }
    }

    private void createSummarySheet(Sheet sheet, List<BuildingInvoiceSummaryDto> summaries, 
                                   CellStyle headerStyle, CellStyle currencyStyle) {
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Tòa nhà", "Mã tòa", "Trạng thái", "Số hóa đơn", "Tổng tiền (VNĐ)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (BuildingInvoiceSummaryDto summary : summaries) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            row.createCell(colNum++).setCellValue(
                    summary.getBuildingName() != null ? summary.getBuildingName() : 
                    summary.getBuildingCode() != null ? summary.getBuildingCode() : 
                    summary.getBuildingId() != null ? summary.getBuildingId().toString().substring(0, 8) : "");
            row.createCell(colNum++).setCellValue(
                    summary.getBuildingCode() != null ? summary.getBuildingCode() : "");
            row.createCell(colNum++).setCellValue(summary.getStatus());
            row.createCell(colNum++).setCellValue(summary.getInvoiceCount());
            
            Cell amountCell = row.createCell(colNum++);
            amountCell.setCellValue(summary.getTotalAmount() != null ? summary.getTotalAmount().doubleValue() : 0);
            amountCell.setCellStyle(currencyStyle);
        }
    }

    private void createInvoiceHeader(Sheet sheet, int rowNum, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(rowNum);
        String[] headers = {"Mã hóa đơn", "Tòa nhà", "Mã tòa", "Ngày phát hành", "Hạn thanh toán", 
                           "Trạng thái", "Tổng tiền (VNĐ)", "Đơn vị thanh toán"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createInvoiceRow(Sheet sheet, int rowNum, InvoiceDto invoice, 
                                 BuildingInvoiceSummaryDto summary, CellStyle currencyStyle, CellStyle dateStyle,
                                 Map<UUID, String> unitNamesMap) {
        Row row = sheet.createRow(rowNum);
        int colNum = 0;
        
        row.createCell(colNum++).setCellValue(invoice.getCode());
        row.createCell(colNum++).setCellValue(
                summary.getBuildingName() != null ? summary.getBuildingName() : 
                summary.getBuildingCode() != null ? summary.getBuildingCode() : "");
        row.createCell(colNum++).setCellValue(
                summary.getBuildingCode() != null ? summary.getBuildingCode() : "");
        
        if (invoice.getIssuedAt() != null) {
            Cell dateCell = row.createCell(colNum++);
            dateCell.setCellValue(invoice.getIssuedAt().toLocalDate());
            dateCell.setCellStyle(dateStyle);
        } else {
            row.createCell(colNum++).setCellValue("");
        }
        
        if (invoice.getDueDate() != null) {
            Cell dueDateCell = row.createCell(colNum++);
            dueDateCell.setCellValue(invoice.getDueDate());
            dueDateCell.setCellStyle(dateStyle);
        } else {
            row.createCell(colNum++).setCellValue("");
        }
        
        row.createCell(colNum++).setCellValue(invoice.getStatus() != null ? invoice.getStatus().toString() : "");
        
        Cell amountCell = row.createCell(colNum++);
        amountCell.setCellValue(invoice.getTotalAmount() != null ? invoice.getTotalAmount().doubleValue() : 0);
        amountCell.setCellStyle(currencyStyle);
        
        String unitDisplayName = "";
        if (invoice.getPayerUnitId() != null) {
            unitDisplayName = unitNamesMap.getOrDefault(invoice.getPayerUnitId(), invoice.getPayerUnitId().toString());
        }
        row.createCell(colNum++).setCellValue(unitDisplayName);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    private CellStyle createDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("yyyy-mm-dd"));
        return style;
    }

    private void autoSizeColumns(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > 0) {
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                    sheet.autoSizeColumn(i);
                    sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1000, 15000));
                }
            }
        }
    }

    private String generateSheetName(BuildingInvoiceSummaryDto summary) {
        String name = summary.getBuildingName();
        if (name != null && !name.trim().isEmpty()) {
            name = sanitizeSheetName(name);
            if (name.length() <= 31) {
                return name;
            }
        }
        
        String code = summary.getBuildingCode();
        if (code != null && !code.trim().isEmpty()) {
            code = sanitizeSheetName(code);
            if (code.length() <= 31) {
                return code;
            }
        }
        
        if (summary.getBuildingId() != null) {
            String id = summary.getBuildingId().toString().substring(0, 8);
            return "Toa_" + id;
        }
        
        return "Toa_Unknown";
    }

    private String sanitizeSheetName(String name) {
        if (name == null) return "";
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        return sanitized;
    }
}

