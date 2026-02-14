package com.QhomeBase.baseservice.service.exports;

import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitExportService {

    private final UnitRepository unitRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private byte[] buildWorkbookFromUnits(List<Unit> units) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Units");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] headers = {"buildingCode", "code", "floor", "areaM2", "bedrooms", "status", "createdAt"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Unit unit : units) {
                Row row = sheet.createRow(rowNum++);
                
                String buildingCode = "";
                if (unit.getBuilding() != null) {
                    buildingCode = unit.getBuilding().getCode() != null ? unit.getBuilding().getCode() : "";
                }
                row.createCell(0).setCellValue(buildingCode);
                
                row.createCell(1).setCellValue(unit.getCode() != null ? unit.getCode() : "");
                
                if (unit.getFloor() != null) {
                    row.createCell(2).setCellValue(unit.getFloor());
                } else {
                    row.createCell(2).setCellValue("");
                }
                
                if (unit.getAreaM2() != null) {
                    row.createCell(3).setCellValue(unit.getAreaM2().doubleValue());
                } else {
                    row.createCell(3).setCellValue("");
                }
                
                if (unit.getBedrooms() != null) {
                    row.createCell(4).setCellValue(unit.getBedrooms());
                } else {
                    row.createCell(4).setCellValue("");
                }
                
                row.createCell(5).setCellValue(unit.getStatus() != null ? unit.getStatus().name() : "");
                
                if (unit.getCreatedAt() != null) {
                    row.createCell(6).setCellValue(unit.getCreatedAt().format(DATE_FORMATTER));
                } else {
                    row.createCell(6).setCellValue("");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Error exporting units to Excel", e);
            throw new IllegalStateException("Không thể xuất Excel: " + e.getMessage(), e);
        }
    }

    public byte[] exportUnitsToExcel() {
        List<Unit> units = unitRepository.findAllWithBuilding();
        return buildWorkbookFromUnits(units);
    }

    public byte[] exportUnitsByBuildingToExcel(UUID buildingId) {
        List<Unit> units = unitRepository.findAllByBuildingId(buildingId);

        return buildWorkbookFromUnits(units);
    }

    public byte[] exportUnitsByBuildingAndFloorToExcel(UUID buildingId, Integer floorNumber) {
        // Lấy tất cả căn hộ của tòa và lọc theo tầng trong Java
        List<Unit> allUnitsInBuilding = unitRepository.findAllByBuildingId(buildingId);
        List<Unit> unitsOnFloor = allUnitsInBuilding.stream()
                .filter(u -> u.getFloor() != null && floorNumber != null && u.getFloor().equals(floorNumber))
                .toList();
        log.info("Exporting units for building {} and floor {}: {} units found", buildingId, floorNumber, unitsOnFloor.size());
        return buildWorkbookFromUnits(unitsOnFloor);
    }
}

