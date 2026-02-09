package com.QhomeBase.baseservice.service.exports;

import com.QhomeBase.baseservice.dto.AssetDto;
import com.QhomeBase.baseservice.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetExportService {

    private final AssetService assetService;

    public byte[] exportAssetsToExcel(
            String buildingId,
            String unitId,
            String assetType) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            List<AssetDto> assets;

            // Get assets based on filters
            if (unitId != null && !unitId.isBlank()) {
                assets = assetService.getAssetsByUnitId(java.util.UUID.fromString(unitId));
            } else if (buildingId != null && !buildingId.isBlank()) {
                assets = assetService.getAssetsByBuildingId(java.util.UUID.fromString(buildingId));
            } else if (assetType != null && !assetType.isBlank()) {
                assets = assetService
                        .getAssetsByAssetType(com.QhomeBase.baseservice.model.AssetType.valueOf(assetType));
            } else {
                assets = assetService.getAllAssets();
            }

            // Group assets by building code
            Map<String, List<AssetDto>> assetsByBuilding = assets.stream()
                    .collect(Collectors.groupingBy(
                            asset -> asset.buildingCode() != null ? asset.buildingCode() : "Không xác định",
                            LinkedHashMap::new,
                            Collectors.toList()));

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Create a sheet for each building
            for (Map.Entry<String, List<AssetDto>> buildingEntry : assetsByBuilding.entrySet()) {
                String buildingCode = buildingEntry.getKey();
                List<AssetDto> buildingAssets = buildingEntry.getValue();

                // Sheet name limited to 31 characters (Excel limit)
                String sheetName = buildingCode.length() > 31 ? buildingCode.substring(0, 31) : buildingCode;
                Sheet sheet = workbook.createSheet(sheetName);

                // Group assets by asset type within this building
                Map<com.QhomeBase.baseservice.model.AssetType, List<AssetDto>> assetsByType = buildingAssets.stream()
                        .collect(Collectors.groupingBy(
                                AssetDto::assetType,
                                LinkedHashMap::new,
                                Collectors.toList()));

                int rowNum = 0;

                // Create header row
                createHeaderRow(sheet, rowNum++);

                // Create data rows grouped by asset type
                for (Map.Entry<com.QhomeBase.baseservice.model.AssetType, List<AssetDto>> typeEntry : assetsByType
                        .entrySet()) {
                    com.QhomeBase.baseservice.model.AssetType currentAssetType = typeEntry.getKey();
                    List<AssetDto> typeAssets = typeEntry.getValue();

                    // Add section header for asset type
                    Row sectionHeaderRow = sheet.createRow(rowNum++);
                    CellStyle sectionHeaderStyle = sheet.getWorkbook().createCellStyle();
                    Font sectionFont = sheet.getWorkbook().createFont();
                    sectionFont.setBold(true);
                    sectionFont.setFontHeightInPoints((short) 12);
                    sectionHeaderStyle.setFont(sectionFont);
                    sectionHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
                    sectionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                    Cell sectionCell = sectionHeaderRow.createCell(0);
                    sectionCell.setCellValue(
                            "Loại: " + getAssetTypeLabel(currentAssetType) + " (" + typeAssets.size() + " thiết bị)");
                    sectionCell.setCellStyle(sectionHeaderStyle);

                    // Merge cells for section header (7 columns: 0-6)
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                            rowNum - 1, rowNum - 1, 0, 6));

                    // Add data rows for this asset type
                    for (AssetDto asset : typeAssets) {
                        createDataRow(sheet, rowNum++, asset, dateFormatter);
                    }

                    // Add blank row between sections
                    rowNum++;
                }

                autoSizeColumns(sheet);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export assets to Excel", e);
            throw new IllegalStateException("Không thể tạo file Excel: " + e.getMessage(), e);
        }
    }

    private void createHeaderRow(Sheet sheet, int rowNum) {
        Row headerRow = sheet.createRow(rowNum);
        String[] headers = {
                "Mã thiết bị",
                "Tên thiết bị",
                "Model",
                "Mã căn hộ",
                "Ngày lắp đặt",
                "Giá mua",
                "Trạng thái"
        };

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDataRow(Sheet sheet, int rowNum, AssetDto asset, DateTimeFormatter dateFormatter) {
        Row row = sheet.createRow(rowNum);
        int cellNum = 0;

        // Mã thiết bị
        row.createCell(cellNum++).setCellValue(asset.assetCode() != null ? asset.assetCode() : "");

        // Tên thiết bị
        row.createCell(cellNum++).setCellValue(asset.name() != null ? asset.name() : "");

        // Model
        row.createCell(cellNum++).setCellValue(asset.model() != null ? asset.model() : "");

        // Mã căn hộ
        row.createCell(cellNum++).setCellValue(asset.unitCode() != null ? asset.unitCode() : "");

        // Ngày lắp đặt
        if (asset.installedAt() != null) {
            row.createCell(cellNum++).setCellValue(asset.installedAt().format(dateFormatter));
        } else {
            row.createCell(cellNum++).setCellValue("");
        }

        // Giá mua
        if (asset.purchasePrice() != null) {
            row.createCell(cellNum++).setCellValue(asset.purchasePrice().doubleValue());
        } else {
            row.createCell(cellNum++).setCellValue("");
        }

        // Trạng thái
        row.createCell(cellNum++)
                .setCellValue(asset.active() != null && asset.active() ? "Đang hoạt động" : "Ngừng hoạt động");
    }

    private String getAssetTypeLabel(com.QhomeBase.baseservice.model.AssetType assetType) {
        return switch (assetType) {
            // Thiết bị nhà Tắm và Vệ sinh
            case TOILET -> "Bồn cầu";
            case BATHROOM_SINK -> "Chậu rửa nhà tắm";
            case WATER_HEATER -> "Bình nóng lạnh";
            case SHOWER_SYSTEM -> "Hệ sen vòi nhà tắm";
            case BATHROOM_FAUCET -> "Vòi chậu rửa";
            case BATHROOM_LIGHT -> "Đèn nhà tắm";
            case BATHROOM_DOOR -> "Cửa nhà tắm";
            case BATHROOM_ELECTRICAL -> "Hệ thống điện nhà vệ sinh";
            // Thiết bị phòng khách
            case LIVING_ROOM_DOOR -> "Cửa phòng khách";
            case LIVING_ROOM_LIGHT -> "Đèn phòng khách";
            case AIR_CONDITIONER -> "Điều hòa";
            case INTERNET_SYSTEM -> "Hệ thống mạng Internet";
            case FAN -> "Quạt";
            case LIVING_ROOM_ELECTRICAL -> "Hệ thống điện phòng khách";
            // Thiết bị phòng ngủ
            case BEDROOM_ELECTRICAL -> "Hệ thống điện phòng ngủ";
            case BEDROOM_AIR_CONDITIONER -> "Điều hòa phòng ngủ";
            case BEDROOM_DOOR -> "Cửa phòng ngủ";
            case BEDROOM_WINDOW -> "Cửa sổ phòng ngủ";
            // Thiết bị nhà bếp
            case KITCHEN_LIGHT -> "Hệ thống đèn nhà bếp";
            case KITCHEN_ELECTRICAL -> "Hệ thống điện nhà bếp";
            case ELECTRIC_STOVE -> "Bếp điện";
            case KITCHEN_DOOR -> "Cửa bếp và logia";
            // Thiết bị hành lang
            case HALLWAY_LIGHT -> "Hệ thống đèn hành lang";
            case HALLWAY_ELECTRICAL -> "Hệ thống điện hành lang";
            // Khác
            case OTHER -> "Khác";
        };
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
            // Set minimum width to prevent columns from being too narrow
            if (sheet.getColumnWidth(i) < 2000) {
                sheet.setColumnWidth(i, 2000);
            }
        }
    }
}
