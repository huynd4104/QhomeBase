package com.QhomeBase.iamservice.service.exports;

import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountExportService {

    private final UserService userService;
    private final com.QhomeBase.iamservice.client.BaseServiceClient baseServiceClient;

    public byte[] exportAccountsToExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            List<User> allStaff = userService.findStaffWithRoles();
            List<User> allResidents = userService.findResidentAccounts();

            List<UUID> residentUserIds = allResidents.stream().map(User::getId).toList();
            Map<UUID, String> residentPhones = new HashMap<>();

            if (!residentUserIds.isEmpty()) {
                try {
                    List<com.QhomeBase.iamservice.client.BaseServiceClient.ResidentDto> residents = baseServiceClient
                            .getResidentsByUserIds(residentUserIds);
                    residents.forEach(r -> {
                        if (r.userId() != null && r.phone() != null) {
                            residentPhones.put(r.userId(), r.phone());
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to fetch resident phones", e);
                }
            }

            Set<User> allUsers = new HashSet<>();
            allUsers.addAll(allStaff);
            allUsers.addAll(allResidents);

            Map<UserRole, List<User>> usersByRole = new LinkedHashMap<>();

            for (User user : allUsers) {
                if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                    for (UserRole role : user.getRoles()) {
                        usersByRole.computeIfAbsent(role, k -> new ArrayList<>()).add(user);
                    }
                }
            }

            List<UserRole> orderedRoles = Arrays.asList(
                    UserRole.ADMIN,
                    UserRole.ACCOUNTANT,
                    UserRole.TECHNICIAN,
                    UserRole.SUPPORTER,
                    UserRole.RESIDENT,
                    UserRole.UNIT_OWNER);

            for (UserRole role : orderedRoles) {
                List<User> users = usersByRole.get(role);
                if (users != null && !users.isEmpty()) {
                    String sheetName = getSheetName(role);
                    Sheet sheet = workbook.createSheet(sheetName);

                    createHeaderRow(sheet);

                    int rowNum = 1;
                    for (User user : users) {
                        String phone = residentPhones.get(user.getId());
                        if (phone == null) {
                            // Try to get from UserService (which gets from StaffProfile)
                            try {
                                phone = userService.getPhone(user.getId());
                            } catch (Exception ignored) {
                            }
                        }
                        createDataRow(sheet, rowNum++, user, phone);
                    }

                    autoSizeColumns(sheet);
                }
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export accounts to Excel", e);
            throw new IllegalStateException("Không thể tạo file Excel: " + e.getMessage(), e);
        }
    }

    private String getSheetName(UserRole role) {
        return switch (role) {
            case ADMIN -> "Admin";
            case ACCOUNTANT -> "Accountant";
            case TECHNICIAN -> "Technician";
            case SUPPORTER -> "Supporter";
            case RESIDENT -> "Resident";
            case UNIT_OWNER -> "UnitOwner";
        };
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = { "Username", "Email", "Roles", "Phone", "Status" };

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDataRow(Sheet sheet, int rowNum, User user, String phone) {
        Row row = sheet.createRow(rowNum);

        row.createCell(0).setCellValue(user.getUsername());
        row.createCell(1).setCellValue(user.getEmail());

        String rolesStr = user.getRoles() != null
                ? user.getRoles().stream()
                        .map(UserRole::name)
                        .collect(Collectors.joining(", "))
                : "";
        row.createCell(2).setCellValue(rolesStr);

        row.createCell(3).setCellValue(phone != null ? phone : "");

        row.createCell(4).setCellValue(user.isActive() ? "Active" : "Inactive");
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
