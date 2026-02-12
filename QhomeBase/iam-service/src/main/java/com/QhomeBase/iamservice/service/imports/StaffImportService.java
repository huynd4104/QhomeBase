package com.QhomeBase.iamservice.service.imports;

import com.QhomeBase.iamservice.client.BaseServiceClient;
import com.QhomeBase.iamservice.dto.StaffImportResponse;
import com.QhomeBase.iamservice.dto.StaffImportRowResult;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.model.imports.StaffImportRow;
import com.QhomeBase.iamservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffImportService {

    private static final int COL_USERNAME = 0;
    private static final int COL_EMAIL = 1;
    private static final int COL_PASSWORD = 2; // New
    private static final int COL_ROLE = 3;
    private static final int COL_ACTIVE = 4;
    private static final int COL_FULLNAME = 5; // New
    private static final int COL_PHONE = 6; // New
    private static final int COL_NATIONAL_ID = 7; // New
    private static final int COL_ADDRESS = 8; // New

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final UserService userService;
    private final BaseServiceClient baseServiceClient;

    public byte[] generateTemplateWorkbook() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("StaffImport");
            Row header = sheet.createRow(0);
            String[] headers = { "username", "email", "password", "role", "active", "fullName", "phone", "nationalId",
                    "address" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            createInstructionSheet(workbook);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo file template: " + e.getMessage(), e);
        }
    }

    public StaffImportResponse importStaffAccounts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import không được để trống");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Vui lòng sử dụng file Excel định dạng .xlsx");
        }

        List<StaffImportRowResult> rowResults = new ArrayList<>();
        int processedRows = 0;
        int successRows = 0;
        Set<String> seenUsernames = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Không tìm thấy dữ liệu trong file Excel");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                processedRows++;
                int excelRowNumber = i;

                String username = getCellString(row, COL_USERNAME);
                String email = getCellString(row, COL_EMAIL);
                String password = getCellString(row, COL_PASSWORD);
                String rawRole = getCellString(row, COL_ROLE);
                String activeRaw = getCellString(row, COL_ACTIVE);
                Boolean active = getCellBoolean(row, COL_ACTIVE);
                String fullName = getCellString(row, COL_FULLNAME);
                String phone = getCellString(row, COL_PHONE);
                String nationalId = getCellString(row, COL_NATIONAL_ID);
                String address = getCellString(row, COL_ADDRESS);

                String trimmedUsername = username != null ? username.trim().toLowerCase() : "";
                String trimmedEmail = email != null ? email.trim().toLowerCase() : "";

                if (StringUtils.hasText(trimmedUsername) && seenUsernames.contains(trimmedUsername)) {
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber, username, email, password, List.of(), active, fullName, phone, nationalId,
                            address,
                            false, null,
                            "Username (row " + excelRowNumber + ") đã được sử dụng ở dòng khác trong file này"));
                    continue;
                }

                if (StringUtils.hasText(trimmedEmail) && seenEmails.contains(trimmedEmail)) {
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber, username, email, password, List.of(), active, fullName, phone, nationalId,
                            address,
                            false, null,
                            "Email (row " + excelRowNumber + ") đã được sử dụng ở dòng khác trong file này"));
                    continue;
                }

                if (!StringUtils.hasText(rawRole)) {
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber, username, email, password, List.of(), active, fullName, phone, nationalId,
                            address,
                            false, null,
                            "Role (row " + excelRowNumber + ") không được để trống"));
                    continue;
                }

                List<String> roleNames = extractRoleNames(rawRole);

                if (roleNames.isEmpty()) {
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber, username, email, password, List.of(), active, fullName, phone, nationalId,
                            address,
                            false, null,
                            "Role (row " + excelRowNumber + ") không được để trống"));
                    continue;
                }

                if (roleNames.size() > 1) {
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber, username, email, password, roleNames, active, fullName, phone, nationalId,
                            address,
                            false, null,
                            "Role (row " + excelRowNumber + ") chỉ được phép có 1 role, không được có nhiều roles"));
                    continue;
                }

                try {
                    StaffImportRow parsedRow = buildImportRow(excelRowNumber, username, email, password, roleNames,
                            active, activeRaw, fullName, phone, nationalId, address);
                    User created = userService.createStaffAccount(
                            parsedRow.getUsername(),
                            parsedRow.getEmail(),
                            parsedRow.getPassword(),
                            parsedRow.getRoles(),
                            parsedRow.getActive() == null || parsedRow.getActive(),
                            parsedRow.getFullName(),
                            parsedRow.getPhone(),
                            parsedRow.getNationalId(),
                            parsedRow.getAddress());
                    baseServiceClient.syncStaffResident(created.getId(), created.getUsername(), created.getEmail(),
                            parsedRow.getPhone());
                    successRows++;
                    seenUsernames.add(parsedRow.getUsername().toLowerCase());
                    seenEmails.add(parsedRow.getEmail().toLowerCase());
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber,
                            parsedRow.getUsername(),
                            parsedRow.getEmail(),
                            parsedRow.getPassword(),
                            roleNames,
                            parsedRow.getActive(),
                            parsedRow.getFullName(),
                            parsedRow.getPhone(),
                            parsedRow.getNationalId(),
                            parsedRow.getAddress(),
                            true,
                            created.getId(),
                            "Created"));
                } catch (Exception ex) {
                    log.warn("Failed to import staff row {}: {}", excelRowNumber, ex.getMessage());
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber, username, email, password, roleNames, active, fullName, phone, nationalId,
                            address,
                            false, null,
                            ex.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc file Excel: " + e.getMessage(), e);
        }

        int failureRows = processedRows - successRows;
        return new StaffImportResponse(processedRows, successRows, failureRows, rowResults);
    }

    private StaffImportRow buildImportRow(int rowNumber,
            String username,
            String email,
            String password,
            List<String> roleNames,
            Boolean active,
            String activeRaw,
            String fullName,
            String phone,
            String nationalId,
            String address) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username (row " + rowNumber + ") không được để trống");
        }
        String trimmedUsername = username.trim();
        validateUsername(trimmedUsername, rowNumber);

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email (row " + rowNumber + ") không được để trống");
        }
        String trimmedEmail = email.trim();
        validateEmail(trimmedEmail, rowNumber);

        if (!StringUtils.hasText(fullName)) {
            throw new IllegalArgumentException("Full Name (row " + rowNumber + ") không được để trống");
        }

        if (roleNames.isEmpty()) {
            throw new IllegalArgumentException("Role (row " + rowNumber + ") không được để trống");
        }
        if (roleNames.size() > 1) {
            throw new IllegalArgumentException(
                    "Role (row " + rowNumber + ") chỉ được phép có 1 role, không được có nhiều roles");
        }
        validateActive(active, activeRaw, rowNumber);
        List<UserRole> roles = roleNames.stream()
                .map(roleName -> {
                    String trimmedRoleName = roleName.trim().toUpperCase(Locale.ROOT);
                    validateRole(trimmedRoleName, rowNumber);
                    try {
                        UserRole role = UserRole.valueOf(trimmedRoleName);
                        if (role == UserRole.ADMIN) {
                            throw new IllegalArgumentException(
                                    "Không thể tạo tài khoản với role ADMIN tại dòng " + rowNumber);
                        }
                        if (role == UserRole.RESIDENT || role == UserRole.UNIT_OWNER) {
                            throw new IllegalArgumentException(
                                    "Không thể tạo tài khoản staff với role " + role + " tại dòng " + rowNumber);
                        }
                        return role;
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage().contains("ADMIN") || ex.getMessage().contains("RESIDENT")
                                || ex.getMessage().contains("UNIT_OWNER") || ex.getMessage().contains("không hợp lệ")) {
                            throw ex;
                        }
                        throw new IllegalArgumentException("Role không hợp lệ tại dòng " + rowNumber + ": " + roleName
                                + ". Các role hợp lệ: ACCOUNTANT, TECHNICIAN, SUPPORTER");
                    }
                })
                .collect(Collectors.toList());

        return StaffImportRow.builder()
                .rowNumber(rowNumber)
                .username(trimmedUsername)
                .email(trimmedEmail)
                .password(password)
                .roles(roles)
                .active(active)
                .fullName(fullName)
                .phone(phone)
                .nationalId(nationalId)
                .address(address)
                .build();
    }

    private void validateUsername(String username, int rowNumber) {
        if (username.contains(" ")) {
            throw new IllegalArgumentException(
                    String.format("Username (row %d) không được chứa khoảng trắng", rowNumber));
        }

        if (!username.matches("^[a-zA-Z].*")) {
            throw new IllegalArgumentException(
                    String.format("Username (row %d) phải bắt đầu bằng chữ cái (a-z, A-Z)", rowNumber));
        }

        if (username.matches(".*[._]$")) {
            throw new IllegalArgumentException(
                    String.format("Username (row %d) không được kết thúc bằng ký tự đặc biệt (., _)", rowNumber));
        }

        if (!username.matches("^[a-zA-Z0-9._]+$")) {
            throw new IllegalArgumentException(
                    String.format(
                            "Username (row %d) chỉ được chứa chữ cái (a-z, A-Z), số (0-9), dấu gạch dưới (_) và dấu chấm (.)",
                            rowNumber));
        }

        if (username.contains("..")) {
            throw new IllegalArgumentException(
                    String.format("Username (row %d) không được chứa nhiều dấu chấm liên tiếp", rowNumber));
        }

        String forbiddenChars = "&=+<>?/\\|{}[]()*^$#@!%~`";
        for (char c : forbiddenChars.toCharArray()) {
            if (username.indexOf(c) >= 0) {
                throw new IllegalArgumentException(
                        String.format("Username (row %d) không được chứa ký tự bị cấm: %s", rowNumber, c));
            }
        }
    }

    private void validateEmail(String email, int rowNumber) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email (row " + rowNumber + ") không được để trống");
        }

        long atCount = email.chars().filter(ch -> ch == '@').count();
        if (atCount == 0) {
            throw new IllegalArgumentException(
                    String.format("Email (row %d) phải có ký tự @", rowNumber));
        }
        if (atCount > 1) {
            throw new IllegalArgumentException(
                    String.format("Email (row %d) chỉ được có 1 ký tự @", rowNumber));
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    String.format("Email (row %d) không đúng định dạng", rowNumber));
        }

        String localPart = parts[0];
        String domain = parts[1];

        if (!domain.toLowerCase().endsWith(".com")) {
            throw new IllegalArgumentException(
                    String.format("Email (row %d) phải có đuôi .com. Ví dụ: user@example.com", rowNumber));
        }

        String localPartPattern = "^[a-zA-Z0-9._%+-]+$";
        if (!localPart.matches(localPartPattern)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Email (row %d) - phần trước @ chỉ được chứa chữ cái (a-z, A-Z), số (0-9) và các ký tự: . _ %% + -",
                            rowNumber));
        }

        String domainWithoutCom = domain.substring(0, domain.length() - 4);
        String domainPattern = "^[a-zA-Z0-9.-]+$";
        if (!domainWithoutCom.matches(domainPattern)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Email (row %d) - phần domain chỉ được chứa chữ cái (a-z, A-Z), số (0-9) và các ký tự: . -",
                            rowNumber));
        }

        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.com$";
        if (!email.matches(emailPattern)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Email (row %d) không đúng định dạng. Email phải có đuôi .com. Ví dụ: user@example.com",
                            rowNumber));
        }

        if (email.length() > 255) {
            throw new IllegalArgumentException(
                    String.format("Email (row %d) không được vượt quá 255 ký tự", rowNumber));
        }
    }

    private void validateRole(String roleName, int rowNumber) {
        try {
            UserRole.valueOf(roleName);
            if (roleName.equals("Administrator")) {
                throw new IllegalArgumentException(
                        String.format("không thể import tài khoản cho admin"));
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Role không hợp lệ tại dòng %d: '%s'. Các role hợp lệ cho staff: ACCOUNTANT, TECHNICIAN, SUPPORTER",
                            rowNumber, roleName));
        }
    }

    private void validateActive(Boolean active, String activeRaw, int rowNumber) {
        if (active == null) {
            if (activeRaw == null || activeRaw.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Cột 'active' (row %d) không được để trống. Chỉ chấp nhận: true, false, 1, 0, yes, no, y, n",
                                rowNumber));
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "Cột 'active' (row %d) có giá trị không hợp lệ: '%s'. Chỉ chấp nhận: true, false, 1, 0, yes, no, y, n",
                                rowNumber, activeRaw.trim()));
            }
        }
    }

    private String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private Boolean getCellBoolean(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> parseBoolean(cell.getStringCellValue());
            case NUMERIC -> cell.getNumericCellValue() != 0;
            default -> null;
        };
    }

    private Boolean parseBoolean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("^(true|1|yes|y)$")) {
            return Boolean.TRUE;
        }
        if (normalized.matches("^(false|0|no|n)$")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private List<String> extractRoleNames(String rawRoles) {
        if (!StringUtils.hasText(rawRoles)) {
            return List.of();
        }
        return Arrays.stream(rawRoles.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = COL_USERNAME; i <= COL_ADDRESS; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && StringUtils.hasText(getCellString(row, i))) {
                return false;
            }
        }
        return true;
    }
    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Hướng dẫn");
        Row header = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = { "Tên cột", "Mô tả", "Quy tắc / Bắt buộc" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        String[][] instructions = {
                { "username", "Tên đăng nhập hệ thống",
                        "Bắt buộc. Duy nhất. 6-16 ký tự. Chỉ chứa chữ thường, số, dấu '.', '_', '-'." },
                { "email", "Địa chỉ Email", "Bắt buộc. Duy nhất. Phải đúng định dạng email và có đuôi .com." },
                { "password", "Mật khẩu", "Bắt buộc. Tối thiểu 6 ký tự." },
                { "role", "Vai trò",
                        "Bắt buộc. Giá trị hợp lệ: ACCOUNTANT (Kế toán), TECHNICIAN (Kỹ thuật), SUPPORTER (Hỗ trợ). Không dùng ADMIN." },
                { "active", "Trạng thái", "Bắt buộc. Nhập: TRUE (Hoạt động) hoặc FALSE (Khóa)." },
                { "fullName", "Họ và tên", "Bắt buộc." },
                { "phone", "Số điện thoại", "Tùy chọn." },
                { "nationalId", "CMND/CCCD", "Tùy chọn." },
                { "address", "Địa chỉ", "Tùy chọn." }
        };

        int rowIdx = 1;
        for (String[] rowData : instructions) {
            Row row = sheet.createRow(rowIdx++);
            for (int i = 0; i < rowData.length; i++) {
                row.createCell(i).setCellValue(rowData[i]);
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
