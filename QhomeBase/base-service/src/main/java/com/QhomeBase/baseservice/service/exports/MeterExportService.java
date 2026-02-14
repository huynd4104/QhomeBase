package com.QhomeBase.baseservice.service.exports;

import com.QhomeBase.baseservice.model.Meter;
import com.QhomeBase.baseservice.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterExportService {

    private final MeterRepository meterRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public byte[] exportMeters(UUID buildingId) {
        List<Meter> meters = buildingId != null
                ? meterRepository.findByBuildingId(buildingId)
                : meterRepository.findAll();

        Map<String, List<Meter>> groupedByService = new LinkedHashMap<>();
        for (Meter meter : meters) {
            if (meter.getService() == null) continue;
            String serviceKey = meter.getService().getCode() != null
                    ? meter.getService().getCode().toUpperCase()
                    : meter.getService().getId().toString();
            groupedByService.computeIfAbsent(serviceKey, key -> new java.util.ArrayList<>()).add(meter);
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            String[] columns = {"buildingCode", "unitCode", "serviceCode", "serviceName", "meterCode", "active", "installedAt", "removedAt", "createdAt"};
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (Map.Entry<String, List<Meter>> entry : groupedByService.entrySet()) {
                String sheetName = entry.getKey();
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                Sheet sheet = wb.createSheet(sheetName);
                Row header = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }
                int rowIdx = 1;
                for (Meter meter : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(meter.getUnit() != null && meter.getUnit().getBuilding() != null
                            ? meter.getUnit().getBuilding().getCode() : "");
                    row.createCell(1).setCellValue(meter.getUnit() != null ? meter.getUnit().getCode() : "");
                    row.createCell(2).setCellValue(meter.getService() != null ? meter.getService().getCode() : "");
                    row.createCell(3).setCellValue(meter.getService() != null ? meter.getService().getName() : "");
                    row.createCell(4).setCellValue(meter.getMeterCode() != null ? meter.getMeterCode() : "");
                    row.createCell(5).setCellValue(meter.getActive() != null ? meter.getActive().toString() : "");
                    row.createCell(6).setCellValue(formatDate(meter.getInstalledAt()));
                    row.createCell(7).setCellValue(formatDate(meter.getRemovedAt()));
                    row.createCell(8).setCellValue(meter.getCreatedAt() != null ? meter.getCreatedAt().format(DATE_FORMATTER) : "");
                }
                for (int i = 0; i < columns.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Error exporting meters to Excel", e);
            throw new IllegalStateException("Không thể xuất Excel meter", e);
        }
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }
}

