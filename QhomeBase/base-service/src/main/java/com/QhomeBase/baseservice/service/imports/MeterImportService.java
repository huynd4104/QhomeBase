package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.dto.MeterCreateReq;
import com.QhomeBase.baseservice.dto.imports.MeterImportResponse;
import com.QhomeBase.baseservice.dto.imports.MeterImportRowResult;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.ServiceRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.service.MeterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterImportService {

    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final ServiceRepository serviceRepository;
    private final MeterService meterService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DataFormatter FORMATTER = new DataFormatter();

    public MeterImportResponse importMeters(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File trống");
        }
        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ .xlsx");
        }

        MeterImportResponse response = new MeterImportResponse();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Không tìm thấy sheet");
            }
            if (sheet.getLastRowNum() < 1) {
                return response;
            }

            response.setTotalRows(sheet.getLastRowNum());
            Row header = sheet.getRow(0);
            int idxBuildingCode = findColumnIndex(header, "buildingCode");
            int idxUnitCode = findColumnIndex(header, "unitCode");
            int idxServiceCode = findColumnIndex(header, "serviceCode");
            int idxInstalledAt = findColumnIndex(header, "installedAt");

            if (idxBuildingCode < 0 || idxUnitCode < 0 || idxServiceCode < 0 || idxInstalledAt < 0) {
                throw new IllegalArgumentException("Thiếu cột bắt buộc: buildingCode, unitCode, serviceCode");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int excelRow = i;
                String buildingCode = readString(row, idxBuildingCode);
                String unitCode = readString(row, idxUnitCode);
                String serviceCode = readString(row, idxServiceCode);
                LocalDate installedAt = parseDate(readString(row, idxInstalledAt));

                try {
                    UUID buildingId = resolveBuildingId(buildingCode, excelRow);
                    UUID unitId = resolveUnitId(buildingId, unitCode, excelRow);
                    UUID serviceId = resolveServiceId(serviceCode, excelRow);

                    var req = new MeterCreateReq(unitId, serviceId, null, installedAt);
                    meterService.create(req);

                    response.getRows().add(MeterImportRowResult.builder()
                            .rowNumber(excelRow)
                            .success(true)
                            .message("Đã tạo công tơ")
                            .build());
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } catch (Exception ex) {
                    log.warn("Import meter lỗi tại dòng {}: {}", excelRow, ex.getMessage());
                    response.getRows().add(MeterImportRowResult.builder()
                            .rowNumber(excelRow)
                            .success(false)
                            .message(ex.getMessage())
                            .build());
                    response.setErrorCount(response.getErrorCount() + 1);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được file Excel", e);
        }
        return response;
    }

    public byte[] generateTemplateWorkbook() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("meters");
            Row header = sh.createRow(0);
            header.createCell(0).setCellValue("buildingCode");
            header.createCell(1).setCellValue("unitCode");
            header.createCell(2).setCellValue("serviceCode");
            header.createCell(3).setCellValue("installedAt");

            Row sample = sh.createRow(1);
            sample.createCell(0).setCellValue("C");
            sample.createCell(1).setCellValue("C3---01");
            sample.createCell(2).setCellValue("WATER");
            sample.createCell(3).setCellValue(LocalDate.now().format(DATE_FORMATTER));

            Row note = sh.createRow(3);
            note.createCell(0).setCellValue("Lưu ý: Dùng dropdown trong cột serviceCode; chọn đúng mã dịch vụ từ danh sách bên dưới. Meter code sinh tự động.");

            List<com.QhomeBase.baseservice.model.Service> services = serviceRepository.findByActiveAndRequiresMeter(true, true);

            Sheet serviceSheet = wb.createSheet("serviceList");
            serviceSheet.createRow(0).createCell(0).setCellValue("Code");
            serviceSheet.getRow(0).createCell(1).setCellValue("Name");
            int serviceSheetRow = 1;
            for (com.QhomeBase.baseservice.model.Service service : services) {
                Row row = serviceSheet.createRow(serviceSheetRow);
                row.createCell(0).setCellValue(service.getCode());
                row.createCell(1).setCellValue(service.getName());
                serviceSheetRow++;
            }
            wb.setSheetHidden(wb.getSheetIndex(serviceSheet), true);

            Name namedServiceCodes = wb.createName();
            namedServiceCodes.setNameName("SERVICE_CODES");
            int lastServiceIndex = Math.max(serviceSheetRow - 1, 1);
            int lastServiceRow = lastServiceIndex + 1;
            int startServiceRow = 2;
            if (startServiceRow > lastServiceRow) {
                startServiceRow = lastServiceRow;
            }
            namedServiceCodes.setRefersToFormula(String.format("'serviceList'!$A$%d:$A$%d", startServiceRow, lastServiceRow));

            for (int c = 0; c <= 3; c++) sh.autoSizeColumn(c);

            DataValidationHelper helper = sh.getDataValidationHelper();
            DataValidationConstraint constraint = helper.createFormulaListConstraint("SERVICE_CODES");
            CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 2, 2);
            DataValidation validation = helper.createValidation(constraint, addressList);
            validation.setSuppressDropDownArrow(false);
            validation.setShowErrorBox(true);
            validation.createErrorBox("Giá trị không hợp lệ", "Chỉ sử dụng mã dịch vụ trong danh sách.");
            sh.addValidationData(validation);
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được template", e);
        }
    }

    private UUID resolveBuildingId(String buildingCode, int rowNumber) {
        if (buildingCode == null || buildingCode.isBlank()) {
            throw new IllegalArgumentException("buildingCode (row " + rowNumber + ") không được để trống");
        }
        return buildingRepository.findByCode(buildingCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy building: " + buildingCode + " (row " + rowNumber + ")"))
                .getId();
    }

    private UUID resolveUnitId(UUID buildingId, String unitCode, int rowNumber) {
        if (unitCode == null || unitCode.isBlank()) {
            throw new IllegalArgumentException("unitCode (row " + rowNumber + ") không được để trống");
        }
        return unitRepository.findByBuildingIdAndCode(buildingId, unitCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy unit: " + unitCode + " trong tòa (row " + rowNumber + ")"))
                .getId();
    }

    private UUID resolveServiceId(String serviceCode, int rowNumber) {
        if (serviceCode == null || serviceCode.isBlank()) {
            throw new IllegalArgumentException("serviceCode (row " + rowNumber + ") không được để trống");
        }
        return serviceRepository.findByCode(serviceCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ: " + serviceCode + " (row " + rowNumber + ")"))
                .getId();
    }

    private String readString(Row row, int idx) {
        if (idx < 0) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String value = FORMATTER.formatCellValue(cell);
        return value != null ? value.trim() : null;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value.trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("installedAt chưa đúng định dạng yyyy-MM-dd: " + value);
            }
        }
    }

    private int findColumnIndex(Row header, String name) {
        if (header == null) return -1;
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell == null) continue;
            String value = cell.getStringCellValue();
            if (value != null && value.trim().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }
}

