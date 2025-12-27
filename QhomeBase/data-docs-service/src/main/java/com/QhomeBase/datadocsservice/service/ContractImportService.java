package com.QhomeBase.datadocsservice.service;


import com.QhomeBase.datadocsservice.dto.imports.ContractImportResponse;
import com.QhomeBase.datadocsservice.dto.imports.ContractImportRowResult;
import com.QhomeBase.datadocsservice.dto.CreateContractRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractImportService {
    private static final int COL_UNIT_ID = 0;
    private static final int COL_CONTRACT_NUMBER = 1;
    private static final int COL_CONTRACT_TYPE = 2;
    private static final int COL_START_DATE = 3;
    private static final int COL_END_DATE = 4;
    private static final int COL_MONTHLY_RENT = 5;
    private static final int COL_PURCHASE_PRICE = 6;
    private static final int COL_PAYMENT_METHOD = 7;
    private static final int COL_PAYMENT_TERMS = 8;
    private static final int COL_PURCHASE_DATE = 9;
    private static final int COL_NOTES = 10;
    private static final int COL_STATUS = 11;

    private final ContractService contractService;
    public ContractImportResponse importContracts(MultipartFile file, UUID createdBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng hoặc không tồn tại");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ định dạng .xlsx");
        }

        List<ContractImportRowResult> rows = new ArrayList<>();
        int total = 0;
        int success = 0;
        int failure = 0;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Không tìm thấy sheet đầu tiên");
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                total++;
                Row row = sheet.getRow(r);
                if (row == null) {
                    rows.add(ContractImportRowResult.builder()
                            .rowNumber(r + 1)
                            .success(false)
                            .contractNumber(null)
                            .createdContractId(null)
                            .message("Dòng trống")
                            .build());
                    failure++;
                    continue;
                }
                try {
                    UUID unitId = parseUuid(getString(row, COL_UNIT_ID));
                    String contractNumber = getString(row, COL_CONTRACT_NUMBER);
                    String contractType = defaultIfBlank(getString(row, COL_CONTRACT_TYPE), "RENTAL").toUpperCase();
                    LocalDate startDate = parseDate(getString(row, COL_START_DATE));
                    LocalDate endDate = parseOptionalDate(getString(row, COL_END_DATE));
                    BigDecimal monthlyRent = parseOptionalDecimal(getString(row, COL_MONTHLY_RENT));
                    BigDecimal purchasePrice = parseOptionalDecimal(getString(row, COL_PURCHASE_PRICE));
                    String paymentMethod = emptyToNull(getString(row, COL_PAYMENT_METHOD));
                    String paymentTerms = emptyToNull(getString(row, COL_PAYMENT_TERMS));
                    LocalDate purchaseDate = parseOptionalDate(getString(row, COL_PURCHASE_DATE));
                    String notes = emptyToNull(getString(row, COL_NOTES));
                    String status = defaultIfBlank(getString(row, COL_STATUS), "ACTIVE");


                    List<String> errs = new ArrayList<>();
                    if (unitId == null) errs.add("unitId bắt buộc");
                    if (isBlank(contractNumber)) errs.add("contractNumber bắt buộc");
                    if (startDate == null) errs.add("startDate bắt buộc");
                    if (!errs.isEmpty()) {
                        throw new IllegalArgumentException(String.join("; ", errs));
                    }

                    CreateContractRequest req = CreateContractRequest.builder()
                            .unitId(unitId)
                            .contractNumber(contractNumber.trim())
                            .contractType(contractType)
                            .startDate(startDate)
                            .endDate(endDate)
                            .monthlyRent(monthlyRent)
                            .purchasePrice(purchasePrice)
                            .paymentMethod(paymentMethod)
                            .paymentTerms(paymentTerms)
                            .purchaseDate(purchaseDate)
                            .notes(notes)
                            .status(status)
                            .build();

                    var created = contractService.createContract(req, createdBy);
                    rows.add(ContractImportRowResult.builder()
                            .rowNumber(r + 1)
                            .success(true)
                            .contractNumber(contractNumber)
                            .createdContractId(created.getId())
                            .message("OK")
                            .build());
                    success++;
                } catch (IllegalArgumentException ex) {
                    rows.add(ContractImportRowResult.builder()
                            .rowNumber(r + 1)
                            .success(false)
                            .contractNumber(null)
                            .createdContractId(null)
                            .message(ex.getMessage())
                            .build());
                    failure++;
                } catch (Exception ex) {
                    log.error("Lỗi không mong muốn tại dòng {}", r + 1, ex);
                    rows.add(ContractImportRowResult.builder()
                            .rowNumber(r + 1)
                            .success(false)
                            .contractNumber(null)
                            .createdContractId(null)
                            .message("Lỗi không mong muốn: " + ex.getMessage())
                            .build());
                    failure++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file: " + e.getMessage(), e);
        }

        return ContractImportResponse.builder()
                .totalRows(total)
                .successCount(success)
                .failureCount(failure)
                .rows(rows)
                .build();
    }
    static ContractImportRowResult fail(int rowNumber, String message) {
        return ContractImportRowResult.builder()
                .rowNumber(rowNumber)
                .success(false)
                .contractNumber(null)
                .createdContractId(null)
                .message(message)
                .build();
    }
    private String getString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            } else {

                return new java.math.BigDecimal(String.valueOf(cell.getNumericCellValue()))
                        .stripTrailingZeros().toPlainString();
            }
        }
        if (cell.getCellType() == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return cell.getStringCellValue();
            } catch (IllegalStateException e) {
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String emptyToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String defaultIfBlank(String s, String def) {
        return isBlank(s) ? def : s.trim();
    }

    private static UUID parseUuid(String s) {
        if (isBlank(s)) return null;
        try {
            return UUID.fromString(s.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unitId không hợp lệ: " + s);
        }
    }

    private static LocalDate parseDate(String s) {
        if (isBlank(s)) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngày không hợp lệ (yyyy-MM-dd): " + s);
        }
    }

    private static LocalDate parseOptionalDate(String s) {
        if (isBlank(s)) return null;
        return parseDate(s);
    }

    private static BigDecimal parseOptionalDecimal(String s) {
        if (isBlank(s)) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Số tiền không hợp lệ: " + s);
        }
    }

    public byte[] generateTemplateWorkbook() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("ContractImport");
            String[] headers = {
                    "unitId","contractNumber","contractType","startDate","endDate",
                    "monthlyRent","purchasePrice","paymentMethod","paymentTerms",
                    "purchaseDate","notes","status"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // Sample RENTAL
            Row s1 = sheet.createRow(1);
            s1.createCell(COL_UNIT_ID).setCellValue("11111111-1111-1111-1111-111111111111");
            s1.createCell(COL_CONTRACT_NUMBER).setCellValue("CN-RENT-001");
            s1.createCell(COL_CONTRACT_TYPE).setCellValue("RENTAL");
            s1.createCell(COL_START_DATE).setCellValue("2025-01-01");
            s1.createCell(COL_END_DATE).setCellValue("2025-12-31");
            s1.createCell(COL_MONTHLY_RENT).setCellValue(12000000);
            s1.createCell(COL_PURCHASE_PRICE).setCellValue("");
            s1.createCell(COL_PAYMENT_METHOD).setCellValue("BANK_TRANSFER");
            s1.createCell(COL_PAYMENT_TERMS).setCellValue("Pay monthly");
            s1.createCell(COL_PURCHASE_DATE).setCellValue("");
            s1.createCell(COL_NOTES).setCellValue("Rental sample");
            s1.createCell(COL_STATUS).setCellValue("ACTIVE");

           
            Row s2 = sheet.createRow(2);
            s2.createCell(COL_UNIT_ID).setCellValue("22222222-2222-2222-2222-222222222222");
            s2.createCell(COL_CONTRACT_NUMBER).setCellValue("CN-PUR-001");
            s2.createCell(COL_CONTRACT_TYPE).setCellValue("PURCHASE");
            s2.createCell(COL_START_DATE).setCellValue("2025-02-01");
            s2.createCell(COL_END_DATE).setCellValue("");
            s2.createCell(COL_MONTHLY_RENT).setCellValue("");
            s2.createCell(COL_PURCHASE_PRICE).setCellValue(2500000000L);
            s2.createCell(COL_PAYMENT_METHOD).setCellValue("");
            s2.createCell(COL_PAYMENT_TERMS).setCellValue("");
            s2.createCell(COL_PURCHASE_DATE).setCellValue("2025-02-01");
            s2.createCell(COL_NOTES).setCellValue("Purchase sample");
            s2.createCell(COL_STATUS).setCellValue("ACTIVE");

            for (int i = 0; i < headers.length; i++) { sheet.autoSizeColumn(i); }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo template: " + e.getMessage(), e);
        }
    }

}
