package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.imports.BuildingImportResponse;
import com.QhomeBase.baseservice.dto.imports.BuildingImportRowResult;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.service.BuildingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingImportService {

    private final BuildingRepository buildingRepository;
    private final BuildingService buildingService;

    public BuildingImportResponse importBuildings(MultipartFile file, String createdBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File trống");
        }
        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ .xlsx");
        }

        BuildingImportResponse response = BuildingImportResponse.builder().build();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                response.getValidationErrors().add("Không tìm thấy sheet trong file Excel");
                response.setHasValidationErrors(true);
                return response;
            }
            if (sheet.getLastRowNum() < 1) {
                response.getValidationErrors().add("File Excel không có dữ liệu (chỉ có header hoặc file trống)");
                response.setHasValidationErrors(true);
                return response;
            }

            response.setTotalRows(sheet.getLastRowNum());
            Row header = sheet.getRow(0);
            if (header == null) {
                response.getValidationErrors().add("Không tìm thấy dòng header (dòng đầu tiên)");
                response.setHasValidationErrors(true);
                return response;
            }
            
            int idxName = findColumnIndex(header, "name");
            int idxAddress = findColumnIndex(header, "address");
            int idxNumberOfFloors = findColumnIndex(header, "numberOfFloors");
            
            // Kiểm tra và thu thập tất cả lỗi thiếu cột
            if (idxName < 0) {
                response.getValidationErrors().add("Thiếu cột 'name' (bắt buộc)");
            }
            if (idxAddress < 0) {
                response.getValidationErrors().add("Thiếu cột 'address' (bắt buộc)");
            }
            if (idxNumberOfFloors < 0) {
                response.getValidationErrors().add("Thiếu cột 'numberOfFloors' (bắt buộc)");
            }
            
            // Nếu có lỗi validation, dừng lại và trả về response với lỗi
            if (!response.getValidationErrors().isEmpty()) {
                response.setHasValidationErrors(true);
                response.getValidationErrors().add(0, "Template không đúng định dạng. Vui lòng tải template mẫu và kiểm tra lại.");
                return response;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                int excelRow = i + 1;
                String name = readString(r, idxName);
                String address = readString(r, idxAddress);
                Integer numberOfFloors = readInteger(r, idxNumberOfFloors);

                try {
                    String trimmedName = name != null ? name.trim() : null;
                    String trimmedAddress = address != null ? address.trim() : null;
                    
                    if (trimmedName == null || trimmedName.isEmpty()) {
                        throw new IllegalArgumentException("Tên building (row " + excelRow + ") không được để trống");
                    }
                    
                    if (trimmedAddress == null || trimmedAddress.isEmpty()) {
                        throw new IllegalArgumentException("Địa chỉ (row " + excelRow + ") không được để trống");
                    }
                    
                    if (numberOfFloors == null) {
                        throw new IllegalArgumentException("Số tầng (row " + excelRow + ") không được để trống");
                    }
                    
                    List<Building> existingBuildings = buildingRepository.findByNameIgnoreCase(trimmedName);
                    if (!existingBuildings.isEmpty()) {
                        int count = existingBuildings.size();
                        String message = count == 1 
                            ? "Tên building (row " + excelRow + ") đã tồn tại trong hệ thống: " + trimmedName
                            : "Tên building (row " + excelRow + ") đã tồn tại trong hệ thống (" + count + " building có cùng tên): " + trimmedName;
                        throw new IllegalArgumentException(message);
                    }
                    
                    validateBuildingName(trimmedName, excelRow);
                    validateBuildingAddress(trimmedAddress, excelRow);
                    validateNumberOfFloors(numberOfFloors, excelRow);
                    
                    var dto = buildingService.createBuilding(new BuildingCreateReq(trimmedName, trimmedAddress, numberOfFloors), createdBy != null ? createdBy : "import");
                    Building saved = buildingRepository.getBuildingById(dto.id());

                    response.getRows().add(BuildingImportRowResult.builder()
                            .rowNumber(excelRow)
                            .success(true)
                            .message("OK")
                            .buildingId(saved.getId().toString())
                            .code(saved.getCode())
                            .name(saved.getName())
                            .build());
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } catch (Exception ex) {
                    log.warn("Import building lỗi tại dòng {}: {}", excelRow, ex.getMessage());
                    response.getRows().add(BuildingImportRowResult.builder()
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
            Sheet sh = wb.createSheet("buildings");
            Row header = sh.createRow(0);
            header.createCell(0).setCellValue("name");
            header.createCell(1).setCellValue("address");
            header.createCell(2).setCellValue("numberOfFloors");

            Row sample1 = sh.createRow(1);
            sample1.createCell(0).setCellValue("Tòa A");
            sample1.createCell(1).setCellValue("123 Đường ABC, Quận 1");
            sample1.createCell(2).setCellValue(10);

            Row sample2 = sh.createRow(2);
            sample2.createCell(0).setCellValue("Tòa B");
            sample2.createCell(1).setCellValue("456 Đường DEF, Quận 2");
            sample2.createCell(2).setCellValue(15);

            for (int c = 0; c <= 2; c++) sh.autoSizeColumn(c);
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được template", e);
        }
    }

    private int findColumnIndex(Row header, String name) {
        if (header == null) return -1;
        String target = name.toLowerCase(Locale.ROOT).trim();
        short last = header.getLastCellNum();
        for (int i = 0; i < last; i++) {
            Cell cell = header.getCell(i);
            if (cell == null) continue;
            String v = cell.getStringCellValue();
            if (v != null && v.trim().toLowerCase(Locale.ROOT).equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private String readString(Row r, int idx) {
        if (idx < 0) return null;
        Cell c = r.getCell(idx);
        if (c == null) return null;
        String v;
        if (c.getCellType() == CellType.STRING) {
            v = c.getStringCellValue();
        } else if (c.getCellType() == CellType.NUMERIC) {
            v = String.valueOf((long) c.getNumericCellValue());
        } else if (c.getCellType() == CellType.BLANK) {
            return null;
        } else {
            DataFormatter formatter = new DataFormatter();
            v = formatter.formatCellValue(c);
        }
        if (v == null) return null;
        String trimmed = v.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateBuildingName(String name, int rowNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên building (row " + rowNumber + ") không được để trống");
        }
        
        if (name.length() > 255) {
            throw new IllegalArgumentException("Tên building (row " + rowNumber + ") không được vượt quá 255 ký tự");
        }
        
        if (name.length() < 2) {
            throw new IllegalArgumentException("Tên building (row " + rowNumber + ") phải có ít nhất 2 ký tự");
        }
    }

    private void validateBuildingAddress(String address, int rowNumber) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ (row " + rowNumber + ") không được để trống");
        }
        
            if (address.length() > 500) {
                throw new IllegalArgumentException("Địa chỉ (row " + rowNumber + ") không được vượt quá 500 ký tự");
            }
            
            if (address.length() < 5) {
                throw new IllegalArgumentException("Địa chỉ (row " + rowNumber + ") phải có ít nhất 5 ký tự");
        }
    }

    private Integer readInteger(Row r, int idx) {
        if (idx < 0) return null;
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            if (c.getCellType() == CellType.NUMERIC) {
                double numValue = c.getNumericCellValue();
                return (int) numValue;
            } else if (c.getCellType() == CellType.STRING) {
                String strValue = c.getStringCellValue();
                if (strValue == null || strValue.trim().isEmpty()) {
                    return null;
                }
                String trimmed = strValue.trim();
                if (trimmed.isEmpty()) {
                    return null;
                }
                return Integer.parseInt(trimmed);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void validateNumberOfFloors(Integer numberOfFloors, int rowNumber) {
        if (numberOfFloors == null) {
            throw new IllegalArgumentException("Số tầng (row " + rowNumber + ") không được để trống");
        }
        if (numberOfFloors <= 0) {
            throw new IllegalArgumentException("Số tầng (row " + rowNumber + ") phải lớn hơn 0");
        }
        if (numberOfFloors >= 100) {
            throw new IllegalArgumentException("Số tầng (row " + rowNumber + ") phải nhỏ hơn 100");
        }
    }
}


