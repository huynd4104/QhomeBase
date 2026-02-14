package com.QhomeBase.baseservice.service.exports;

import com.QhomeBase.baseservice.dto.AssetDto;
import com.QhomeBase.baseservice.model.AssetType;
import com.QhomeBase.baseservice.model.RoomType;
import com.QhomeBase.baseservice.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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

    public byte[] exportAssetsToExcel(String buildingId, String unitId, String assetType) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            List<AssetDto> assets;

            // 1. Lấy dữ liệu theo bộ lọc
            if (unitId != null && !unitId.isBlank()) {
                assets = assetService.getAssetsByUnitId(java.util.UUID.fromString(unitId));
            } else if (buildingId != null && !buildingId.isBlank()) {
                assets = assetService.getAssetsByBuildingId(java.util.UUID.fromString(buildingId));
            } else if (assetType != null && !assetType.isBlank()) {
                assets = assetService.getAssetsByAssetType(AssetType.valueOf(assetType));
            } else {
                assets = assetService.getAllAssets();
            }

            // 2. Nhóm tài sản theo Tòa nhà (Building Code)
            Map<String, List<AssetDto>> assetsByBuilding = assets.stream()
                    .collect(Collectors.groupingBy(
                            asset -> asset.buildingCode() != null ? asset.buildingCode() : "N/A",
                            TreeMap::new, // Sắp xếp tên tòa nhà
                            Collectors.toList()));

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // 3. Tạo Sheet cho từng tòa nhà
            for (Map.Entry<String, List<AssetDto>> buildingEntry : assetsByBuilding.entrySet()) {
                String buildingCode = buildingEntry.getKey();
                List<AssetDto> buildingAssets = buildingEntry.getValue();

                // Tên Sheet không quá 31 ký tự
                String sheetName = buildingCode.length() > 31 ? buildingCode.substring(0, 31) : buildingCode;
                Sheet sheet = workbook.createSheet(sheetName);

                // 4. Sắp xếp danh sách: Tầng -> Mã Căn Hộ -> Loại Phòng -> Tên TB
                buildingAssets.sort(Comparator.comparing((AssetDto a) -> a.floor() != null ? a.floor() : 0)
                        .thenComparing(a -> a.unitCode() != null ? a.unitCode() : "")
                        .thenComparing(a -> a.roomType() != null ? a.roomType().name() : "")
                        .thenComparing(AssetDto::name));

                int rowNum = 0;

                // 5. Tạo dòng tiêu đề cột
                createHeaderRow(sheet, rowNum++);

                // 6. Duyệt và ghi dữ liệu (Có phân nhóm Visual theo Tầng)
                Integer currentFloor = null;

                for (AssetDto asset : buildingAssets) {
                    // Nếu đổi tầng -> Tạo dòng Header ngăn cách
                    if (!Objects.equals(asset.floor(), currentFloor)) {
                        currentFloor = asset.floor();
                        createFloorSeparatorRow(sheet, rowNum++, currentFloor, workbook);
                    }
                    // Ghi dòng dữ liệu chi tiết
                    createDataRow(sheet, rowNum++, asset, dateFormatter);
                }

                // 7. Tự động chỉnh độ rộng cột
                autoSizeColumns(sheet);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export assets to Excel", e);
            throw new IllegalStateException("Lỗi xuất file Excel: " + e.getMessage(), e);
        }
    }

    // --- CÁC HÀM PHỤ TRỢ (HELPER METHODS) ---

    private void createHeaderRow(Sheet sheet, int rowNum) {
        Row headerRow = sheet.createRow(rowNum);
        // Đã bỏ cột "Giá mua", thêm Brand, Serial, Warranty, Description
        String[] headers = {
                "Mã Căn Hộ",      // 0
                "Phòng/Khu vực",  // 1
                "Loại thiết bị",  // 2
                "Mã Tài Sản",     // 3
                "Tên Tài Sản",    // 4
                "Thương Hiệu",    // 5
                "Model",          // 6
                "S/N (Serial)",   // 7
                "Ngày Lắp Đặt",   // 8
                "Hạn Bảo Hành",   // 9
                "Trạng Thái",     // 10
                "Mô Tả / Ghi Chú" // 11
        };

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createFloorSeparatorRow(Sheet sheet, int rowNum, Integer floor, Workbook workbook) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);

        String floorText = (floor == null) ? "Tầng chưa xác định" : "TẦNG " + floor;
        cell.setCellValue(floorText);

        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex()); // Màu nền phân cách tầng
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);

        cell.setCellStyle(style);

        // Merge cells ngang hết bảng (từ cột 0 đến 11)
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 11));
    }

    private void createDataRow(Sheet sheet, int rowNum, AssetDto asset, DateTimeFormatter dateFormatter) {
        Row row = sheet.createRow(rowNum);
        int col = 0;

        // Common style cho border
        CellStyle borderStyle = sheet.getWorkbook().createCellStyle();
        borderStyle.setBorderBottom(BorderStyle.THIN);
        borderStyle.setBorderTop(BorderStyle.THIN);
        borderStyle.setBorderLeft(BorderStyle.THIN);
        borderStyle.setBorderRight(BorderStyle.THIN);

        // Helper để tạo cell nhanh
        createCell(row, col++, asset.unitCode(), borderStyle);
        createCell(row, col++, getRoomTypeLabel(asset.roomType()), borderStyle);
        createCell(row, col++, getAssetTypeLabel(asset.assetType()), borderStyle);
        createCell(row, col++, asset.assetCode(), borderStyle);
        createCell(row, col++, asset.name(), borderStyle);
        createCell(row, col++, asset.brand(), borderStyle);         // Full info
        createCell(row, col++, asset.model(), borderStyle);         // Full info
        createCell(row, col++, asset.serialNumber(), borderStyle);  // Full info

        // Ngày lắp đặt
        createCell(row, col++, asset.installedAt() != null ? asset.installedAt().format(dateFormatter) : "", borderStyle);

        // Hạn bảo hành
        createCell(row, col++, asset.warrantyUntil() != null ? asset.warrantyUntil().format(dateFormatter) : "", borderStyle);

        // Trạng thái
        String status = (asset.active() != null && asset.active()) ? "Đang sử dụng" : "Ngừng sử dụng";
        createCell(row, col++, status, borderStyle);

        // Mô tả / Ghi chú
        createCell(row, col++, asset.description(), borderStyle);
    }

    private void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet) {
        // Tự động chỉnh độ rộng cho 12 cột
        for (int i = 0; i <= 11; i++) {
            sheet.autoSizeColumn(i);
            // Giới hạn độ rộng tối thiểu và tối đa cho đẹp
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth < 3000) sheet.setColumnWidth(i, 3000);
            if (currentWidth > 10000) sheet.setColumnWidth(i, 10000);
        }
    }

    private String getRoomTypeLabel(RoomType roomType) {
        if (roomType == null) return "";
        return switch (roomType) {
            case LIVING_ROOM -> "Phòng Khách";
            case BEDROOM -> "Phòng Ngủ";
            case KITCHEN -> "Nhà Bếp";
            case BATHROOM -> "Nhà Tắm/WC";
            case HALLWAY -> "Hành Lang";
            default -> roomType.name();
        };
    }

    private String getAssetTypeLabel(com.QhomeBase.baseservice.model.AssetType assetType) {
        if (assetType == null) return "";
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
}