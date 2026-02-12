package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.residentview.*;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.QhomeBase.baseservice.model.*;
import java.io.InputStream;
import java.util.Iterator;
import org.springframework.web.multipart.MultipartFile;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResidentViewService {

    private final HouseholdRepository householdRepository;
    private final com.QhomeBase.baseservice.repository.ResidentRepository residentRepository;
    private final com.QhomeBase.baseservice.repository.UnitRepository unitRepository;
    private final com.QhomeBase.baseservice.repository.HouseholdMemberRepository householdMemberRepository;
    private final com.QhomeBase.baseservice.repository.BuildingRepository buildingRepository;

    public List<ResidentViewYearDto> getYears() {
        LocalDate minDate = householdRepository.findMinStartDate();
        LocalDate maxDate = householdRepository.findMaxEndDate();

        if (minDate == null) {
            return Collections.emptyList();
        }

        if (maxDate == null) {
            maxDate = LocalDate.now();
        } else if (maxDate.isAfter(LocalDate.now())) {
            // If max date is in future, maybe we still want to show it? keeping it as is.
        }

        int startYear = minDate.getYear();
        int endYear = maxDate.getYear();
        // Ensure we at least include current year if data is old, or just stick to
        // data?
        // Let's stick to data range.

        List<ResidentViewYearDto> years = new ArrayList<>();

        for (int year = endYear; year >= startYear; year--) {
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);

            // We need total residents and occupied units for this year.
            // Reusing findBuildingsByYear logic but aggregating?
            // Or simpler: just count.
            // Let's try to aggregate from buildings for now to ensure consistency,
            // though specific count queries would be faster.
            // Given the hierarchy, let's just use the buildings query and sum up.
            List<ResidentViewBuildingDto> buildings = householdRepository.findBuildingsByYear(yearStart, yearEnd);

            long totalResidents = buildings.stream().mapToLong(ResidentViewBuildingDto::getTotalResidents).sum();
            long occupiedUnits = buildings.stream().mapToLong(ResidentViewBuildingDto::getOccupiedUnits).sum();

            years.add(ResidentViewYearDto.builder()
                    .year(year)
                    .totalResidents(totalResidents)
                    .occupiedUnits(occupiedUnits)
                    .build());
        }

        return years;
    }

    public List<ResidentViewBuildingDto> getBuildingsByYear(Integer year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        return householdRepository.findBuildingsByYear(yearStart, yearEnd);
    }

    public List<ResidentViewFloorDto> getFloorsByYearAndBuilding(Integer year, UUID buildingId) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        return householdRepository.findFloorsByYearAndBuilding(yearStart, yearEnd, buildingId);
    }

    public List<ResidentViewUnitDto> getUnitsByYearBuildingAndFloor(Integer year, UUID buildingId, Integer floor) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        return householdRepository.findUnitsByYearBuildingAndFloor(yearStart, yearEnd, buildingId, floor);
    }

    public List<ResidentViewResidentDto> getResidentsByUnitAndYear(UUID unitId, Integer year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        return householdRepository.findResidentsByUnitAndYear(yearStart, yearEnd, unitId);
    }

    public byte[] exportResidents(Integer year, UUID buildingId, Integer floor) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<ResidentExportDto> data = householdRepository.findResidentsForExport(year, yearStart, yearEnd, buildingId,
                floor);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Residents " + year);

            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = { "Year", "Building Code", "Unit Code", "Full Name", "Phone", "Email", "National ID",
                    "DOB", "Status", "Party Type", "Relation", "Is Primary", "Start Date", "Joined At" };

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowNum = 1;
            for (ResidentExportDto dto : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getYear() != null ? dto.getYear() : year);
                row.createCell(1).setCellValue(dto.getBuildingCode());
                row.createCell(2).setCellValue(dto.getUnitCode());
                row.createCell(3).setCellValue(dto.getFullName());
                row.createCell(4).setCellValue(dto.getPhone());
                row.createCell(5).setCellValue(dto.getEmail());
                row.createCell(6).setCellValue(dto.getNationalId());
                row.createCell(7).setCellValue(dto.getDob() != null ? dto.getDob().toString() : "");
                row.createCell(8).setCellValue(dto.getStatus());
                row.createCell(9).setCellValue(dto.getPartyType());
                row.createCell(10).setCellValue(dto.getRelation());
                row.createCell(11).setCellValue(dto.getIsPrimary() != null && dto.getIsPrimary() ? "Yes" : "No");
                row.createCell(12).setCellValue(dto.getStartDate() != null ? dto.getStartDate().toString() : "");
                row.createCell(13).setCellValue(dto.getJoinedAt() != null ? dto.getJoinedAt().toString() : "");
            }

            // Auto size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error creating Excel export", e);
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    public byte[] downloadTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Instructions Sheet
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
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            int rowNum = 0;
            Row titleRow = instructionSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HƯỚNG DẪN IMPORT CƯ DÂN");
            titleCell.setCellStyle(titleStyle);

            rowNum++; // Spacer

            String[][] instructions = {
                    { "Cột", "Mô tả", "Bắt buộc", "Lưu ý" },
                    { "Building Code", "Mã tòa nhà", "Có", "Phải tồn tại trong hệ thống (VD: T01)." },
                    { "Unit Code", "Mã căn hộ", "Có", "Phải tồn tại trong tòa nhà tương ứng (VD: A0101)." },
                    { "Full Name", "Họ và tên", "Có", "Tên đầy đủ của cư dân." },
                    { "Phone", "Số điện thoại", "Có",
                            "Dùng để định danh. Nếu trùng SĐT hệ thống sẽ báo lỗi nếu tên khác." },
                    { "Email", "Email liên hệ", "Không", "" },
                    { "National ID", "CMND/CCCD/Passport", "Không", "Nếu điền phải là duy nhất trong hệ thống." },
                    { "DOB", "Ngày sinh", "Không", "Định dạng: yyyy-MM-dd (VD: 1990-01-01)." },
                    { "Status", "Trạng thái cư trú", "Có", "ACTIVE (Đang ở), TEMPORARY (Tạm trú), INACTIVE (Đã rời)." },
                    { "Party Type", "Loại cư dân", "Có", "RESIDENT (Cư dân), TENANT (Khách thuê), GUEST (Khách)." },
                    { "Relation", "Quan hệ với chủ hộ", "Có",
                            "HEAD (Chủ hộ), WIFE (Vợ), HUSBAND (Chồng), SON (Con trai), DAUGHTER (Con gái), PARENT (Cha mẹ), OTHER (Khác)." },
                    { "Is Primary", "Là người đại diện", "Có",
                            "YES (Có) hoặc NO (Không). Mỗi hộ nên có 1 người đại diện." },
                    { "Start Date", "Ngày bắt đầu ở", "Không",
                            "Định dạng: yyyy-MM-dd. Nếu để trống sẽ lấy ngày hiện tại." },
                    { "Joined At", "Ngày chuyển vào", "Không",
                            "Định dạng: yyyy-MM-dd. Nếu để trống sẽ lấy ngày hiện tại." }
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

            // Data Sheet
            Sheet sheet = workbook.createSheet("Residents");
            Row headerRow = sheet.createRow(0);
            String[] headers = { "Building Code", "Unit Code", "Full Name", "Phone", "Email", "National ID",
                    "DOB (yyyy-mm-dd)", "Status", "Party Type", "Relation", "Is Primary", "Start Date (yyyy-mm-dd)",
                    "Joined At (yyyy-mm-dd)" };

            CellStyle dataHeaderStyle = workbook.createCellStyle();
            dataHeaderStyle.setFont(headerFont);
            dataHeaderStyle
                    .setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            dataHeaderStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(dataHeaderStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error creating template", e);
            throw new RuntimeException("Failed to create template", e);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public byte[] importResidents(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream);
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                Workbook errorWorkbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.getSheet("Residents");
            // If "Residents" sheet not found, try to find first data sheet (skip
            // Instructions)
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
            // Fallback to first sheet if still null
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
            throw new RuntimeException("Failed to import residents", e);
        }
    }

    private void processImportRow(Row row, DataFormatter dataFormatter) {
        // Helper to get string value
        java.util.function.Function<Integer, String> getVal = idx -> {
            Cell cell = row.getCell(idx);
            if (cell == null)
                return null;
            return dataFormatter.formatCellValue(cell).trim();
        };

        String buildingCode = getVal.apply(0);
        String unitCode = getVal.apply(1);
        String fullName = getVal.apply(2);
        String phone = getVal.apply(3);
        String email = getVal.apply(4);
        String nationalId = getVal.apply(5);
        String dobStr = getVal.apply(6);
        String statusStr = getVal.apply(7);
        String partyTypeStr = getVal.apply(8);
        String relation = getVal.apply(9);
        String isPrimaryStr = getVal.apply(10);
        String startDateStr = getVal.apply(11);
        String joinedAtStr = getVal.apply(12);

        if (buildingCode == null || buildingCode.isEmpty())
            throw new IllegalArgumentException("Building Code is required");
        if (unitCode == null || unitCode.isEmpty())
            throw new IllegalArgumentException("Unit Code is required");
        if (fullName == null || fullName.isEmpty())
            throw new IllegalArgumentException("Full Name is required");
        if (partyTypeStr == null || partyTypeStr.isEmpty())
            throw new IllegalArgumentException("Party Type is required");

        // Parse Enums
        HouseholdKind kind;
        try {
            kind = HouseholdKind.valueOf(partyTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Party Type: " + partyTypeStr);
        }

        ResidentStatus status = ResidentStatus.ACTIVE;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = ResidentStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Status: " + statusStr);
            }
        }

        // Parse Dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dob = parseDate(dobStr, formatter, "DOB");
        LocalDate startDate = parseDate(startDateStr, formatter, "Start Date");
        LocalDate joinedAt = parseDate(joinedAtStr, formatter, "Joined At");
        if (startDate == null)
            startDate = LocalDate.now(); // Default start date?
        if (joinedAt == null)
            joinedAt = LocalDate.now();

        // Find Building
        com.QhomeBase.baseservice.model.Building building = buildingRepository.findByCode(buildingCode)
                .orElseThrow(() -> new IllegalArgumentException("Building not found: " + buildingCode));

        Unit unit = unitRepository.findByBuildingIdAndCode(building.getId(), unitCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unit not found: " + unitCode + " in building " + buildingCode));

        System.out.println("DEBUG IMPORT: Found Unit " + unit.getCode() + " (ID: " + unit.getId() + ")");

        // Resident
        Resident resident;
        boolean residentExists = false;
        if (phone != null && !phone.isEmpty()) {
            if (residentRepository.existsByPhone(phone)) {
                resident = residentRepository.findByPhone(phone).get();
                residentExists = true;
                System.out.println("DEBUG IMPORT: Found existing resident by phone: " + phone + " (Name: "
                        + resident.getFullName() + ", ID: " + resident.getId() + ")");

                // Check if existing data matches import data
                if (!fullName.equals(resident.getFullName())) {
                    throw new IllegalArgumentException("Resident with phone " + phone
                            + " already exists with different name: " + resident.getFullName());
                }

                // If National ID provided in import, check if it matches existing
                if (resident.getNationalId() != null && nationalId != null
                        && !nationalId.equals(resident.getNationalId())) {
                    throw new IllegalArgumentException(
                            "Resident with phone " + phone + " has different National ID: " + resident.getNationalId());
                }

                // If DOB provided, check if it matches
                if (resident.getDob() != null && dob != null && !dob.equals(resident.getDob())) {
                    throw new IllegalArgumentException(
                            "Resident with phone " + phone + " has different DOB: " + resident.getDob());
                }

            } else {
                resident = new Resident();
                resident.setPhone(phone);
                resident.setFullName(fullName);
                resident.setEmail(email);
                resident.setNationalId(nationalId);
                resident.setDob(dob);
                resident.setStatus(status);
                residentRepository.save(resident);
                System.out
                        .println("DEBUG IMPORT: Created new resident: " + fullName + " (ID: " + resident.getId() + ")");
            }
        } else {
            // If no phone, maybe check National ID?
            if (nationalId != null && !nationalId.isEmpty() && residentRepository.existsByNationalId(nationalId)) {
                resident = residentRepository.findByNationalId(nationalId).get();
                residentExists = true;
                System.out.println("DEBUG IMPORT: Found existing resident by National ID: " + nationalId + " (Name: "
                        + resident.getFullName() + ", ID: " + resident.getId() + ")");

                // Check name conflict
                if (!fullName.equals(resident.getFullName())) {
                    throw new IllegalArgumentException("Resident with National ID " + nationalId
                            + " already exists with different name: " + resident.getFullName());
                }

                // Check DOB conflict
                if (resident.getDob() != null && dob != null && !dob.equals(resident.getDob())) {
                    throw new IllegalArgumentException(
                            "Resident with National ID " + nationalId + " has different DOB: " + resident.getDob());
                }
            } else {
                throw new IllegalArgumentException("Phone or National ID required for new resident");
            }
        }

        // Household
        // Prioritize finding an exact match by start date to avoid overlap conflicts
        List<Household> existingHouseholds = householdRepository.findByUnitIdAndKind(unit.getId(), kind);

        final LocalDate targetStartDate = startDate;
        Household household = existingHouseholds.stream()
                .filter(h -> h.getStartDate().equals(targetStartDate))
                .findFirst()
                .orElse(null);

        // If no exact start date match, try to find a current active one
        if (household == null) {
            household = existingHouseholds.stream()
                    .filter(h -> h.getEndDate() == null || h.getEndDate().isAfter(LocalDate.now()))
                    .findFirst()
                    .orElse(null);
        }

        if (household == null) {
            household = new Household();
            household.setUnitId(unit.getId());
            household.setKind(kind);
            household.setStartDate(startDate);
            householdRepository.save(household);
            System.out.println("DEBUG IMPORT: Created NEW Household " + household.getId());
        } else {
            System.out.println("DEBUG IMPORT: Reusing Household " + household.getId() + " (Start: "
                    + household.getStartDate() + ", End: " + household.getEndDate() + ")");
            if (household.getEndDate() != null) {
                household.setEndDate(null);
                householdRepository.save(household);
                System.out.println("DEBUG IMPORT: Re-activated Household (Set EndDate to null)");
            }
        }

        // Member
        // Check if member exists (active or inactive) and update/reactivate
        Optional<HouseholdMember> existingMemberOpt = householdMemberRepository
                .findByHouseholdIdAndResidentId(household.getId(), resident.getId());

        if (existingMemberOpt.isPresent()) {
            HouseholdMember member = existingMemberOpt.get();
            boolean needSave = false;

            // Reactivate if left
            if (member.getLeftAt() != null) {
                member.setLeftAt(null);
                needSave = true;
                System.out.println("DEBUG IMPORT: Cleared LeftAt for existing member");
            }

            // Update relation/primary status if changed?
            // For now, let's assume we update them to match import file
            if (!member.getRelation().equals(relation)) {
                member.setRelation(relation);
                needSave = true;
            }
            // Parse boolean properly
            boolean newIsPrimary = Boolean.parseBoolean(isPrimaryStr) || "1".equals(isPrimaryStr)
                    || "YES".equalsIgnoreCase(isPrimaryStr);
            if (member.getIsPrimary() != newIsPrimary) {
                member.setIsPrimary(newIsPrimary);
                needSave = true;
            }

            if (needSave) {
                householdMemberRepository.save(member);
                System.out.println("DEBUG IMPORT: Updated existing member " + member.getId());
            } else {
                System.out.println("DEBUG IMPORT: Member already up-to-date");
            }

        } else {
            HouseholdMember member = new HouseholdMember();
            member.setHouseholdId(household.getId());
            member.setResidentId(resident.getId());
            member.setRelation(relation);
            member.setIsPrimary(Boolean.parseBoolean(isPrimaryStr) || "1".equals(isPrimaryStr)
                    || "YES".equalsIgnoreCase(isPrimaryStr));
            member.setJoinedAt(joinedAt);
            householdMemberRepository.save(member);
            System.out.println("DEBUG IMPORT: Created NEW Member " + member.getId());
        }
    }

    private LocalDate parseDate(String dateStr, DateTimeFormatter formatter, String fieldName) {
        if (dateStr == null || dateStr.isEmpty())
            return null;
        try {
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " format: " + dateStr);
        }
    }
}
