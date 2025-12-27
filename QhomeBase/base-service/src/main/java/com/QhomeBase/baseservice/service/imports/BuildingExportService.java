package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.BuildingRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingExportService {

    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportBuildingsToExcel() {
        List<Building> buildings = buildingRepository.findAllByOrderByCodeAsc();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Buildings");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] headers = {"code", "name", "address", "numberOfFloors", "status", "createdAt", "createdBy"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Building building : buildings) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(building.getCode() != null ? building.getCode() : "");
                row.createCell(1).setCellValue(building.getName() != null ? building.getName() : "");
                row.createCell(2).setCellValue(building.getAddress() != null ? building.getAddress() : "");
                
                if (building.getNumberOfFloors() != null) {
                    row.createCell(3).setCellValue(building.getNumberOfFloors());
                } else {
                    row.createCell(3).setCellValue("");
                }
                
                row.createCell(4).setCellValue(building.getStatus() != null ? building.getStatus().name() : "");
                
                if (building.getCreatedAt() != null) {
                    row.createCell(5).setCellValue(building.getCreatedAt().format(DATE_FORMATTER));
                } else {
                    row.createCell(5).setCellValue("");
                }
                
                row.createCell(6).setCellValue(building.getCreatedBy() != null ? building.getCreatedBy() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Error exporting buildings to Excel", e);
            throw new IllegalStateException("Không thể xuất Excel: " + e.getMessage(), e);
        }
    }

    public byte[] exportBuildingsWithUnitsToExcel() {
        List<Building> buildings = buildingRepository.findAllByOrderByCodeAsc();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle buildingInfoStyle = wb.createCellStyle();
            Font buildingInfoFont = wb.createFont();
            buildingInfoFont.setBold(true);
            buildingInfoFont.setFontHeightInPoints((short) 12);
            buildingInfoStyle.setFont(buildingInfoFont);

            for (Building building : buildings) {
                String sheetName = building.getCode() != null && !building.getCode().isEmpty() 
                    ? building.getCode() 
                    : "Building_" + building.getId().toString().substring(0, 8);
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                Sheet sheet = wb.createSheet(sheetName);

                int rowNum = 0;

                Row buildingHeaderRow = sheet.createRow(rowNum++);
                buildingHeaderRow.createCell(0).setCellValue("Thông tin Tòa nhà");
                buildingHeaderRow.getCell(0).setCellStyle(buildingInfoStyle);

                Row buildingInfoRow1 = sheet.createRow(rowNum++);
                buildingInfoRow1.createCell(0).setCellValue("Mã tòa:");
                buildingInfoRow1.createCell(1).setCellValue(building.getCode() != null ? building.getCode() : "");
                buildingInfoRow1.createCell(3).setCellValue("Tên tòa:");
                buildingInfoRow1.createCell(4).setCellValue(building.getName() != null ? building.getName() : "");

                Row buildingInfoRow2 = sheet.createRow(rowNum++);
                buildingInfoRow2.createCell(0).setCellValue("Địa chỉ:");
                buildingInfoRow2.createCell(1).setCellValue(building.getAddress() != null ? building.getAddress() : "");
                buildingInfoRow2.createCell(3).setCellValue("Số tầng:");
                if (building.getNumberOfFloors() != null) {
                    buildingInfoRow2.createCell(4).setCellValue(building.getNumberOfFloors());
                } else {
                    buildingInfoRow2.createCell(4).setCellValue("");
                }

                Row buildingInfoRow3 = sheet.createRow(rowNum++);
                buildingInfoRow3.createCell(0).setCellValue("Trạng thái:");
                buildingInfoRow3.createCell(1).setCellValue(building.getStatus() != null ? building.getStatus().name() : "");

                rowNum++;

                List<Unit> units = unitRepository.findAllByBuildingId(building.getId());
                if (!units.isEmpty()) {
                    Row unitHeaderRow = sheet.createRow(rowNum++);
                    unitHeaderRow.createCell(0).setCellValue("Danh sách Căn hộ/Phòng");
                    unitHeaderRow.getCell(0).setCellStyle(buildingInfoStyle);

                    Row unitTableHeader = sheet.createRow(rowNum++);
                    String[] unitHeaders = {"code", "floor", "areaM2", "bedrooms", "status", "createdAt"};
                    for (int i = 0; i < unitHeaders.length; i++) {
                        Cell cell = unitTableHeader.createCell(i);
                        cell.setCellValue(unitHeaders[i]);
                        cell.setCellStyle(headerStyle);
                    }

                    for (Unit unit : units) {
                        Row unitRow = sheet.createRow(rowNum++);
                        unitRow.createCell(0).setCellValue(unit.getCode() != null ? unit.getCode() : "");
                        
                        if (unit.getFloor() != null) {
                            unitRow.createCell(1).setCellValue(unit.getFloor());
                        } else {
                            unitRow.createCell(1).setCellValue("");
                        }
                        
                        if (unit.getAreaM2() != null) {
                            unitRow.createCell(2).setCellValue(unit.getAreaM2().doubleValue());
                        } else {
                            unitRow.createCell(2).setCellValue("");
                        }
                        
                        if (unit.getBedrooms() != null) {
                            unitRow.createCell(3).setCellValue(unit.getBedrooms());
                        } else {
                            unitRow.createCell(3).setCellValue("");
                        }
                        
                        unitRow.createCell(4).setCellValue(unit.getStatus() != null ? unit.getStatus().name() : "");
                        
                        if (unit.getCreatedAt() != null) {
                            unitRow.createCell(5).setCellValue(unit.getCreatedAt().format(DATE_FORMATTER));
                        } else {
                            unitRow.createCell(5).setCellValue("");
                        }
                    }
                } else {
                    Row noUnitRow = sheet.createRow(rowNum++);
                    noUnitRow.createCell(0).setCellValue("Chưa có căn hộ/phòng");
                }

                for (int i = 0; i < 6; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Error exporting buildings with units to Excel", e);
            throw new IllegalStateException("Không thể xuất Excel: " + e.getMessage(), e);
        }
    }
}

