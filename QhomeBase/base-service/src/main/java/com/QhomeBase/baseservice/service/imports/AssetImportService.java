package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.dto.imports.AssetImportResponse;
import com.QhomeBase.baseservice.dto.imports.AssetImportRowResult;
import com.QhomeBase.baseservice.model.Asset;
import com.QhomeBase.baseservice.model.AssetType;
import com.QhomeBase.baseservice.model.RoomType;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.AssetRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetImportService {

    private final AssetRepository assetRepository;
    private final UnitRepository unitRepository;

    @Transactional
    public AssetImportResponse importAssets(MultipartFile file, UUID buildingId) {
        AssetImportResponse response = new AssetImportResponse();
        List<AssetImportRowResult> rowResults = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null || isRowEmpty(row)) continue;

                    String assetCode = getCellValue(row.getCell(3));
                    String assetName = getCellValue(row.getCell(4));

                    try {
                        processRow(row, buildingId);

                        rowResults.add(AssetImportRowResult.builder()
                                .rowNumber(rowIndex + 1)
                                .assetCode(assetCode)
                                .assetName(assetName)
                                .success(true)
                                .errorMessage(null)
                                .build());
                        response.setSuccessCount(response.getSuccessCount() + 1);

                    } catch (Exception e) {
                        // Ghi nhận lỗi dòng này
                        rowResults.add(AssetImportRowResult.builder()
                                .rowNumber(rowIndex + 1)
                                .assetCode(assetCode != null ? assetCode : "N/A")
                                .assetName(assetName)
                                .success(false)
                                .errorMessage(e.getMessage())
                                .build());
                        response.setErrorCount(response.getErrorCount() + 1);
                        log.warn("Lỗi import dòng {}: {}", rowIndex + 1, e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            validationErrors.add("Lỗi đọc file Excel: " + e.getMessage());
            response.setHasValidationErrors(true);
        } catch (Exception e) {
            validationErrors.add("Lỗi hệ thống khi import: " + e.getMessage());
            response.setHasValidationErrors(true);
        }

        response.setRows(rowResults);
        response.setValidationErrors(validationErrors);
        response.setTotalRows(response.getSuccessCount() + response.getErrorCount());

        return response;
    }

    public byte[] generateTemplateWorkbook() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Mẫu nhập tài sản");

            // 1. Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Mã Căn Hộ (*)",      // 0
                    "Phòng/Khu vực",      // 1
                    "Loại thiết bị",      // 2
                    "Mã Tài Sản (*)",     // 3
                    "Tên Tài Sản",        // 4
                    "Thương Hiệu",        // 5
                    "Model",              // 6
                    "S/N (Serial)",       // 7
                    "Ngày Lắp Đặt (dd/MM/yyyy)", // 8
                    "Hạn Bảo Hành (dd/MM/yyyy)", // 9
                    "Trạng Thái (SD/Ngừng)",     // 10
                    "Mô Tả / Ghi Chú"            // 11
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // 2. Dropdowns
            String[] assetTypes = {
                    "Bồn cầu", "Chậu rửa", "Bình nóng lạnh", "Sen vòi", "Đèn nhà tắm",
                    "Điều hòa", "Tủ lạnh", "Tivi", "Sofa", "Bàn", "Ghế", "Giường",
                    "Tủ quần áo", "Tủ bếp", "Bếp điện", "Máy giặt", "Modem/Wifi",
                    "Rèm cửa", "Đệm", "Đèn", "Khác"
            };
            createDropdown(sheet, assetTypes, 2);

            String[] roomTypes = {"Phòng Khách", "Phòng Ngủ", "Nhà Bếp", "Nhà Tắm/WC", "Ban Công", "Hành Lang", "Khác"};
            createDropdown(sheet, roomTypes, 1);

            String[] statusTypes = {"Đang sử dụng", "Ngừng sử dụng", "Hỏng", "Bảo trì"};
            createDropdown(sheet, statusTypes, 10);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi tạo file mẫu: " + e.getMessage());
        }
    }

    // --- Private Methods ---

    private void processRow(Row row, UUID buildingId) {
        String unitCode = getCellValue(row.getCell(0));
        if (unitCode == null || unitCode.isEmpty()) throw new IllegalArgumentException("Thiếu Mã căn hộ");

        Unit unit = unitRepository.findByBuildingIdAndCode(buildingId, unitCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy căn hộ mã '" + unitCode + "' trong tòa nhà này."));

        String assetCode = getCellValue(row.getCell(3));
        if (assetCode == null || assetCode.isEmpty()) throw new IllegalArgumentException("Thiếu Mã tài sản");

        Asset asset = assetRepository.findByAssetCode(assetCode).orElse(new Asset());

        if (asset.getId() == null) {
            asset.setAssetCode(assetCode);
            asset.setUnit(unit);
        } else {
            if (!asset.getUnit().getId().equals(unit.getId())) {
                throw new IllegalArgumentException("Mã tài sản " + assetCode + " đã tồn tại ở căn hộ khác.");
            }
        }

        asset.setRoomType(parseRoomType(getCellValue(row.getCell(1))));
        asset.setAssetType(parseAssetType(getCellValue(row.getCell(2))));
        asset.setName(getCellValue(row.getCell(4)));
        asset.setBrand(getCellValue(row.getCell(5)));
        asset.setModel(getCellValue(row.getCell(6)));
        asset.setSerialNumber(getCellValue(row.getCell(7)));
        asset.setInstalledAt(getDateValue(row.getCell(8)));
        asset.setWarrantyUntil(getDateValue(row.getCell(9)));

        String status = getCellValue(row.getCell(10));
        asset.setActive(!"Ngừng sử dụng".equalsIgnoreCase(status) && !"Hỏng".equalsIgnoreCase(status));

        asset.setDescription(getCellValue(row.getCell(11)));

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

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private LocalDate getDateValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                return LocalDate.parse(cell.getStringCellValue(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private AssetType parseAssetType(String text) {
        if (text == null) return AssetType.OTHER;
        String normalized = text.toLowerCase().trim();

        // --- NHÓM PHÒNG NGỦ (Check trước vì cụ thể hơn) ---
        if (normalized.contains("điện phòng ngủ")) return AssetType.BEDROOM_ELECTRICAL;
        if (normalized.contains("điều hòa phòng ngủ")) return AssetType.BEDROOM_AIR_CONDITIONER;
        if (normalized.contains("cửa sổ") || normalized.contains("cửa sổ phòng ngủ")) return AssetType.BEDROOM_WINDOW;
        if (normalized.contains("cửa phòng ngủ")) return AssetType.BEDROOM_DOOR;

        // --- NHÓM NHÀ BẾP ---
        if (normalized.contains("đèn nhà bếp") || normalized.contains("đèn bếp")) return AssetType.KITCHEN_LIGHT;
        if (normalized.contains("điện nhà bếp") || normalized.contains("điện bếp")) return AssetType.KITCHEN_ELECTRICAL;
        if (normalized.contains("bếp điện") || normalized.contains("bếp từ")) return AssetType.ELECTRIC_STOVE;
        if (normalized.contains("cửa bếp") || normalized.contains("logia")) return AssetType.KITCHEN_DOOR;

        // --- NHÓM HÀNH LANG ---
        if (normalized.contains("đèn hành lang")) return AssetType.HALLWAY_LIGHT;
        if (normalized.contains("điện hành lang")) return AssetType.HALLWAY_ELECTRICAL;

        // --- NHÓM PHÒNG KHÁCH (Check các item cụ thể của phòng khách) ---
        if (normalized.contains("cửa phòng khách")) return AssetType.LIVING_ROOM_DOOR;
        if (normalized.contains("đèn phòng khách")) return AssetType.LIVING_ROOM_LIGHT;
        if (normalized.contains("điện phòng khách")) return AssetType.LIVING_ROOM_ELECTRICAL;
        if (normalized.contains("internet") || normalized.contains("wifi") || normalized.contains("mạng")) return AssetType.INTERNET_SYSTEM;
        if (normalized.contains("quạt")) return AssetType.FAN;

        // --- NHÓM VỆ SINH / NHÀ TẮM ---
        if (normalized.contains("bồn cầu") || normalized.contains("toilet")) return AssetType.TOILET;
        if (normalized.contains("vòi chậu")) return AssetType.BATHROOM_FAUCET; // Check trước 'chậu rửa'
        if (normalized.contains("chậu rửa") || normalized.contains("lavabo")) return AssetType.BATHROOM_SINK;
        if (normalized.contains("nóng lạnh") || normalized.contains("bình nước nóng")) return AssetType.WATER_HEATER;
        if (normalized.contains("sen vòi") || normalized.contains("hệ sen") || normalized.contains("vòi sen")) return AssetType.SHOWER_SYSTEM;
        if (normalized.contains("đèn nhà tắm") || normalized.contains("đèn vệ sinh")) return AssetType.BATHROOM_LIGHT;
        if (normalized.contains("cửa nhà tắm") || normalized.contains("cửa vệ sinh")) return AssetType.BATHROOM_DOOR;
        if (normalized.contains("điện nhà vệ sinh") || normalized.contains("điện nhà tắm")) return AssetType.BATHROOM_ELECTRICAL;

        // --- CÁC THIẾT BỊ CHUNG (Nếu không khớp nhóm cụ thể ở trên) ---
        if (normalized.contains("điều hòa")) return AssetType.AIR_CONDITIONER; // Mặc định là điều hòa phòng khách/chung

        // Fallback: Thử map chính xác tên Enum (cho trường hợp admin nhập tiếng Anh)
        try {
            return AssetType.valueOf(text.toUpperCase().trim().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return AssetType.OTHER;
        }
    }

    private RoomType parseRoomType(String text) {
        if (text == null) return RoomType.OTHER;
        String normalized = text.toLowerCase().trim();
        if (normalized.contains("khách")) return RoomType.LIVING_ROOM;
        if (normalized.contains("ngủ")) return RoomType.BEDROOM;
        if (normalized.contains("hành lang")) return RoomType.HALLWAY;
        if (normalized.contains("vệ sinh")) return RoomType.BATHROOM;
        if (normalized.contains("bếp")) return RoomType.KITCHEN;
        return RoomType.OTHER;
    }
}