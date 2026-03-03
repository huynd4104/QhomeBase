package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.model.Asset;
import com.QhomeBase.baseservice.model.AssetType;
import com.QhomeBase.baseservice.model.RoomType;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.AssetRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.service.exports.AssetExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetImportService {

    private final AssetRepository assetRepository;
    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;

    // =========================================================================
    // 1. GENERATE TEMPLATE (giống Resident: có sheet "Hướng dẫn" + sheet Data)
    // =========================================================================
    public byte[] generateTemplateWorkbook() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // --- Sheet 1: Hướng dẫn ---
            Sheet instructionSheet = workbook.createSheet("Hướng dẫn");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            int rowNum = 0;
            Row titleRow = instructionSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HƯỚNG DẪN IMPORT TÀI SẢN / THIẾT BỊ");
            titleCell.setCellStyle(titleStyle);

            rowNum++; // Spacer

            String[][] instructions = {
                    { "Cột", "Mô tả", "Bắt buộc", "Lưu ý" },
                    { "Building Code", "Mã tòa nhà", "Có", "Phải tồn tại trong hệ thống (VD: T01)." },
                    { "Unit Code", "Mã căn hộ", "Có", "Phải tồn tại trong tòa nhà tương ứng (VD: A0101)." },
                    { "Phòng/Khu vực", "Loại phòng lắp đặt", "Có",
                            "Phòng Khách, Phòng Ngủ, Nhà Bếp, Nhà Tắm/WC, Hành Lang, Khác." },
                    { "Loại thiết bị", "Loại thiết bị", "Có",
                            "Bồn cầu, Chậu rửa, Bình nóng lạnh, Sen vòi, Vòi nước, Đèn, Cửa, Cửa sổ, Hệ thống điện, Điều hòa, Internet, Quạt, Bếp điện, Khác." },
                    { "Mã Tài Sản", "Mã tài sản (unique)", "Có", "Mã duy nhất trong hệ thống. Nếu trùng sẽ cập nhật." },
                    { "Tên Tài Sản", "Tên thiết bị", "Không", "Tên mô tả thiết bị." },
                    { "Thương Hiệu", "Hãng sản xuất", "Không", "VD: Toto, Panasonic, Daikin..." },
                    { "Model", "Model/Mã sản phẩm", "Không", "" },
                    { "S/N (Serial)", "Số serial", "Không", "" },
                    { "Ngày Lắp Đặt", "Ngày lắp đặt", "Không", "Định dạng: dd/MM/yyyy (VD: 01/06/2024)." },
                    { "Hạn Bảo Hành", "Ngày hết bảo hành", "Không", "Định dạng: dd/MM/yyyy (VD: 01/06/2026)." },
                    { "Trạng Thái", "Trạng thái sử dụng", "Không", "Đang sử dụng (mặc định), Ngừng sử dụng." },
                    { "Mô Tả / Ghi Chú", "Ghi chú thêm", "Không", "" },
            };

            for (int i = 0; i < instructions.length; i++) {
                Row row = instructionSheet.createRow(rowNum++);
                for (int j = 0; j < instructions[i].length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(instructions[i][j]);
                    if (i == 0) {
                        cell.setCellStyle(headerStyle);
                    }
                }
            }

            for (int i = 0; i < 4; i++)
                instructionSheet.autoSizeColumn(i);

            // --- Sheet 2: Data (Tài sản) ---
            Sheet dataSheet = workbook.createSheet("Tài sản");
            Row dataHeaderRow = dataSheet.createRow(0);
            String[] headers = {
                    "Building Code (*)", // 0
                    "Unit Code (*)", // 1
                    "Phòng/Khu vực (*)", // 2
                    "Loại thiết bị (*)", // 3
                    "Mã Tài Sản (*)", // 4
                    "Tên Tài Sản", // 5
                    "Thương Hiệu", // 6
                    "Model", // 7
                    "S/N (Serial)", // 8
                    "Ngày Lắp Đặt (dd/MM/yyyy)", // 9
                    "Hạn Bảo Hành (dd/MM/yyyy)", // 10
                    "Trạng Thái", // 11
                    "Mô Tả / Ghi Chú" // 12
            };

            CellStyle dataHeaderStyle = workbook.createCellStyle();
            dataHeaderStyle.setFont(headerFont);
            dataHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            dataHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dataHeaderStyle.setBorderBottom(BorderStyle.THIN);
            dataHeaderStyle.setBorderTop(BorderStyle.THIN);
            dataHeaderStyle.setBorderLeft(BorderStyle.THIN);
            dataHeaderStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = dataHeaderRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(dataHeaderStyle);
                dataSheet.setColumnWidth(i, 5000);
            }

            // Dropdowns
            String[] roomTypes = { "Phòng Khách", "Phòng Ngủ", "Nhà Bếp", "Nhà Tắm/WC", "Hành Lang", "Khác" };
            createDropdown(dataSheet, roomTypes, 2);

            String[] assetTypes = {
                    "Bồn cầu", "Chậu rửa", "Bình nóng lạnh", "Sen vòi", "Vòi nước",
                    "Đèn", "Cửa", "Cửa sổ", "Hệ thống điện", "Điều hòa",
                    "Internet", "Quạt", "Bếp điện", "Khác"
            };
            createDropdown(dataSheet, assetTypes, 3);

            String[] statusTypes = { "Đang sử dụng", "Ngừng sử dụng" };
            createDropdown(dataSheet, statusTypes, 11);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi tạo file mẫu: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. IMPORT (giống Resident: trả byte[] error report Excel)
    // =========================================================================
    @Transactional
    public byte[] importAssets(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream);
             ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
             Workbook errorWorkbook = new XSSFWorkbook()) {

            // Tìm sheet data (bỏ qua sheet Hướng dẫn)
            Sheet sheet = workbook.getSheet("Tài sản");
            if (sheet == null) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet s = workbook.getSheetAt(i);
                    String sName = s.getSheetName();
                    if (!sName.equalsIgnoreCase("Hướng dẫn") && !sName.equalsIgnoreCase("README")
                            && !sName.equalsIgnoreCase("Instruction")) {
                        sheet = s;
                        break;
                    }
                }
            }
            if (sheet == null)
                sheet = workbook.getSheetAt(0);

            Sheet errorSheet = errorWorkbook.createSheet("Errors");
            Row errorHeader = errorSheet.createRow(0);
            errorHeader.createCell(0).setCellValue("Row Number");
            errorHeader.createCell(1).setCellValue("Error Message");
            int errorRowNum = 1;

            Iterator<Row> rows = sheet.iterator();
            // Skip header
            if (rows.hasNext())
                rows.next();

            int rowCount = 1;
            boolean hasErrors = false;
            DataFormatter dataFormatter = new DataFormatter();

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                rowCount++;

                // Skip empty rows
                boolean isEmptyRow = true;
                int lastCellNum = currentRow.getLastCellNum();
                for (int i = 0; i < lastCellNum; i++) {
                    Cell cell = currentRow.getCell(i);
                    if (cell != null && cell.getCellType() != CellType.BLANK
                            && !dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                        isEmptyRow = false;
                        break;
                    }
                }
                if (isEmptyRow)
                    continue;

                try {
                    processImportRow(currentRow, dataFormatter);
                } catch (Exception e) {
                    hasErrors = true;
                    Row errorRow = errorSheet.createRow(errorRowNum++);
                    errorRow.createCell(0).setCellValue(rowCount);
                    errorRow.createCell(1).setCellValue(e.getMessage());
                }
            }

            if (hasErrors) {
                errorSheet.autoSizeColumn(0);
                errorSheet.autoSizeColumn(1);
                errorWorkbook.write(errorStream);
                return errorStream.toByteArray();
            }
            return new byte[0];
        } catch (IOException e) {
            throw new RuntimeException("Lỗi import tài sản: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE METHODS
    // =========================================================================

    private void processImportRow(Row row, DataFormatter dataFormatter) {
        java.util.function.Function<Integer, String> getVal = idx -> {
            Cell cell = row.getCell(idx);
            if (cell == null)
                return null;
            return dataFormatter.formatCellValue(cell).trim();
        };

        String buildingCode = getVal.apply(0);
        String unitCode = getVal.apply(1);
        String roomTypeStr = getVal.apply(2);
        String assetTypeStr = getVal.apply(3);
        String assetCode = getVal.apply(4);
        String assetName = getVal.apply(5);
        String brand = getVal.apply(6);
        String model = getVal.apply(7);
        String serialNumber = getVal.apply(8);
        String installedAtStr = getVal.apply(9);
        String warrantyUntilStr = getVal.apply(10);
        String statusStr = getVal.apply(11);
        String description = getVal.apply(12);

        // Validate required fields
        if (buildingCode == null || buildingCode.isEmpty())
            throw new IllegalArgumentException("Building Code là bắt buộc");
        if (unitCode == null || unitCode.isEmpty())
            throw new IllegalArgumentException("Unit Code là bắt buộc");
        if (assetCode == null || assetCode.isEmpty())
            throw new IllegalArgumentException("Mã Tài Sản là bắt buộc");

        // Find Building & Unit
        var building = buildingRepository.findByCode(buildingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tòa nhà: " + buildingCode));

        Unit unit = unitRepository.findByBuildingIdAndCode(building.getId(), unitCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy căn hộ " + unitCode + " trong tòa nhà " + buildingCode));

        // Parse enums
        RoomType roomType = parseRoomType(roomTypeStr);
        AssetType assetType = parseAssetType(assetTypeStr);

        // Parse dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate installedAt = parseDate(installedAtStr, formatter, "Ngày Lắp Đặt");
        LocalDate warrantyUntil = parseDate(warrantyUntilStr, formatter, "Hạn Bảo Hành");

        // Find or create asset
        Asset asset = assetRepository.findByAssetCode(assetCode).orElse(new Asset());

        if (asset.getId() == null) {
            // New asset
            asset.setAssetCode(assetCode);
            asset.setUnit(unit);
        } else {
            // Existing asset - check same unit
            if (!asset.getUnit().getId().equals(unit.getId())) {
                throw new IllegalArgumentException("Mã tài sản " + assetCode + " đã tồn tại ở căn hộ khác.");
            }
        }

        asset.setRoomType(roomType);
        asset.setAssetType(assetType);
        asset.setName(assetName);
        asset.setBrand(brand);
        asset.setModel(model);
        asset.setSerialNumber(serialNumber);
        asset.setInstalledAt(installedAt);
        asset.setWarrantyUntil(warrantyUntil);
        asset.setDescription(description);

        // Status
        if (statusStr != null && !statusStr.isEmpty()) {
            asset.setActive(!"Ngừng sử dụng".equalsIgnoreCase(statusStr));
        } else {
            if (asset.getActive() == null)
                asset.setActive(true);
        }

        assetRepository.save(asset);
    }

    private void createDropdown(Sheet sheet, String[] options, int colIndex) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(options);
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, colIndex, colIndex);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private LocalDate parseDate(String dateStr, DateTimeFormatter formatter, String fieldName) {
        if (dateStr == null || dateStr.isEmpty())
            return null;
        try {
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Sai định dạng " + fieldName + ": " + dateStr + " (yêu cầu dd/MM/yyyy)");
        }
    }

    private AssetType parseAssetType(String text) {
        if (text == null || text.isEmpty())
            return AssetType.OTHER;
        String normalized = text.toLowerCase().trim();

        if (normalized.contains("bồn cầu") || normalized.contains("toilet"))
            return AssetType.TOILET;
        if (normalized.contains("chậu rửa") || normalized.contains("lavabo"))
            return AssetType.SINK;
        if (normalized.contains("nóng lạnh") || normalized.contains("bình nước nóng"))
            return AssetType.WATER_HEATER;
        if (normalized.contains("sen vòi") || normalized.contains("hệ sen") || normalized.contains("vòi sen"))
            return AssetType.SHOWER_SYSTEM;
        if (normalized.contains("vòi") || normalized.contains("faucet"))
            return AssetType.FAUCET;
        if (normalized.contains("đèn") || normalized.contains("light"))
            return AssetType.LIGHT;
        if (normalized.contains("cửa sổ") || normalized.contains("window"))
            return AssetType.WINDOW;
        if (normalized.contains("cửa") || normalized.contains("door"))
            return AssetType.DOOR;
        if (normalized.contains("điện") || normalized.contains("electrical"))
            return AssetType.ELECTRICAL_SYSTEM;
        if (normalized.contains("điều hòa") || normalized.contains("điều hoà"))
            return AssetType.AIR_CONDITIONER;
        if (normalized.contains("internet") || normalized.contains("wifi") || normalized.contains("mạng"))
            return AssetType.INTERNET_SYSTEM;
        if (normalized.contains("quạt") || normalized.contains("fan"))
            return AssetType.FAN;
        if (normalized.contains("bếp điện") || normalized.contains("bếp từ"))
            return AssetType.ELECTRIC_STOVE;

        // Fallback: try exact enum name
        try {
            return AssetType.valueOf(text.toUpperCase().trim().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return AssetType.OTHER;
        }
    }

    private RoomType parseRoomType(String text) {
        if (text == null || text.isEmpty())
            return RoomType.OTHER;
        String normalized = text.toLowerCase().trim();
        if (normalized.contains("khách"))
            return RoomType.LIVING_ROOM;
        if (normalized.contains("ngủ"))
            return RoomType.BEDROOM;
        if (normalized.contains("hành lang"))
            return RoomType.HALLWAY;
        if (normalized.contains("tắm") || normalized.contains("vệ sinh") || normalized.contains("wc"))
            return RoomType.BATHROOM;
        if (normalized.contains("bếp"))
            return RoomType.KITCHEN;
        return RoomType.OTHER;
    }
}