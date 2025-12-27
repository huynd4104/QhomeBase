package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.model.InvoiceLine;
import com.QhomeBase.financebillingservice.repository.InvoiceLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceExportService {

    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceService invoiceService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportInvoicesToExcel(
            String serviceCode,
            String status,
            UUID unitId,
            UUID buildingId,
            String startDate,
            String endDate) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            List<InvoiceDto> invoices = invoiceService.getAllInvoicesForAdmin(
                    serviceCode, status, unitId, buildingId, startDate, endDate);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            Map<String, List<InvoiceDto>> invoicesByService = new HashMap<>();
            
            for (InvoiceDto invoice : invoices) {
                List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
                
                if (lines.isEmpty()) {
                    invoicesByService.computeIfAbsent("OTHER", k -> new ArrayList<>()).add(invoice);
                } else {
                    for (InvoiceLine line : lines) {
                        String service = line.getServiceCode() != null ? line.getServiceCode() : "OTHER";
                        invoicesByService.computeIfAbsent(service, k -> new ArrayList<>()).add(invoice);
                    }
                }
            }

            Sheet summarySheet = workbook.createSheet("Tổng hợp");
            createSummarySheet(summarySheet, invoices, invoicesByService, headerStyle, dataStyle, currencyStyle);

            for (Map.Entry<String, List<InvoiceDto>> entry : invoicesByService.entrySet()) {
                String serviceCodeKey = entry.getKey();
                List<InvoiceDto> serviceInvoices = entry.getValue();
                
                List<InvoiceDto> uniqueInvoices = serviceInvoices.stream()
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());
                
                String sheetName = getServiceSheetName(serviceCodeKey);
                Sheet sheet = workbook.createSheet(sheetName);

                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                String[] headers = {
                        "STT", "Mã HĐ", "Ngày phát hành", "Hạn thanh toán", "Người thanh toán",
                        "Địa chỉ", "Mô tả", "Số lượng", "Đơn giá", "Thành tiền",
                        "Trạng thái", "Ngày thanh toán", "Phương thức", "Mã giao dịch"
                };
                
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                int stt = 1;
                for (InvoiceDto invoice : uniqueInvoices) {
                    List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
                    
                    List<InvoiceLine> filteredLines = lines.stream()
                            .filter(line -> {
                                String lineService = line.getServiceCode() != null ? line.getServiceCode() : "OTHER";
                                return lineService.equals(serviceCodeKey);
                            })
                            .collect(java.util.stream.Collectors.toList());
                    
                    if (filteredLines.isEmpty()) {
                        Row row = sheet.createRow(rowNum++);
                        createInvoiceRow(row, invoice, null, stt++, dataStyle, currencyStyle, dateStyle, false);
                    } else {
                        for (InvoiceLine line : filteredLines) {
                            Row row = sheet.createRow(rowNum++);
                            createInvoiceRow(row, invoice, line, stt++, dataStyle, currencyStyle, dateStyle, false);
                            stt--;
                        }
                        stt++;
                    }
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting invoices to Excel", e);
            throw new IllegalStateException("Không thể xuất Excel danh sách hóa đơn", e);
        }
    }

    private void createInvoiceRow(Row row, InvoiceDto invoice, InvoiceLine line, int stt,
                                  CellStyle dataStyle, CellStyle currencyStyle, CellStyle dateStyle, boolean includeServiceColumn) {
        int colNum = 0;

        createCell(row, colNum++, String.valueOf(stt), dataStyle);
        createCell(row, colNum++, invoice.getCode() != null ? invoice.getCode() : "", dataStyle);

        if (invoice.getIssuedAt() != null) {
            createCell(row, colNum++, invoice.getIssuedAt().format(DATETIME_FORMATTER), dateStyle);
        } else {
            createCell(row, colNum++, "", dataStyle);
        }

        if (invoice.getDueDate() != null) {
            createCell(row, colNum++, invoice.getDueDate().format(DATE_FORMATTER), dateStyle);
        } else {
            createCell(row, colNum++, "", dataStyle);
        }

        createCell(row, colNum++, invoice.getBillToName() != null ? invoice.getBillToName() : "", dataStyle);
        createCell(row, colNum++, invoice.getBillToAddress() != null ? invoice.getBillToAddress() : "", dataStyle);

        if (includeServiceColumn) {
            if (line != null && line.getServiceCode() != null) {
                String serviceName = getServiceName(line.getServiceCode());
                createCell(row, colNum++, serviceName, dataStyle);
            } else {
                createCell(row, colNum++, "", dataStyle);
            }
        }

        if (line != null && line.getDescription() != null) {
            createCell(row, colNum++, line.getDescription(), dataStyle);
        } else {
            createCell(row, colNum++, "", dataStyle);
        }

        if (line != null && line.getQuantity() != null) {
            createCell(row, colNum++, line.getQuantity().toString() + " " + (line.getUnit() != null ? line.getUnit() : ""), dataStyle);
        } else {
            createCell(row, colNum++, "", dataStyle);
        }

        if (line != null && line.getUnitPrice() != null) {
            createCell(row, colNum++, line.getUnitPrice().doubleValue(), currencyStyle);
        } else {
            createCell(row, colNum++, "", dataStyle);
        }

        if (line != null && line.getLineTotal() != null) {
            createCell(row, colNum++, line.getLineTotal().doubleValue(), currencyStyle);
        } else if (invoice.getTotalAmount() != null) {
            createCell(row, colNum++, invoice.getTotalAmount().doubleValue(), currencyStyle);
        } else {
            createCell(row, colNum++, "", dataStyle);
        }

        String statusName = getStatusName(invoice.getStatus() != null ? invoice.getStatus().name() : "");
        createCell(row, colNum++, statusName, dataStyle);

        if (invoice.getPaidAt() != null) {
            createCell(row, colNum++, invoice.getPaidAt().format(DATETIME_FORMATTER), dateStyle);
        } else {
            createCell(row, colNum++, "", dataStyle);
        }

        createCell(row, colNum++, invoice.getPaymentGateway() != null ? invoice.getPaymentGateway() : "", dataStyle);
        createCell(row, colNum++, invoice.getVnpTransactionRef() != null ? invoice.getVnpTransactionRef() : "", dataStyle);
    }

    private void createCell(Row row, int colNum, String value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int colNum, double value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("dd/mm/yyyy"));
        return style;
    }

    private String getServiceName(String serviceCode) {
        if (serviceCode == null) return "";
        switch (serviceCode.toUpperCase()) {
            case "ELECTRICITY":
                return "Điện";
            case "WATER":
                return "Nước";
            case "VEHICLE_CARD":
                return "Thẻ xe";
            case "ELEVATOR_CARD":
                return "Thẻ thang máy";
            case "RESIDENT_CARD":
                return "Thẻ cư dân";
            default:
                return serviceCode;
        }
    }

    private String getStatusName(String status) {
        if (status == null) return "";
        switch (status.toUpperCase()) {
            case "DRAFT":
                return "Nháp";
            case "PUBLISHED":
                return "Đã phát hành";
            case "PAID":
                return "Đã thanh toán";
            case "VOID":
                return "Đã hủy";
            default:
                return status;
        }
    }

    private String getServiceSheetName(String serviceCode) {
        if (serviceCode == null || serviceCode.equals("OTHER")) {
            return "Khác";
        }
        return getServiceName(serviceCode);
    }

    private void createSummarySheet(Sheet sheet, List<InvoiceDto> allInvoices, 
                                    Map<String, List<InvoiceDto>> invoicesByService,
                                    CellStyle headerStyle, CellStyle dataStyle, CellStyle currencyStyle) {
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO TỔNG HỢP HÓA ĐƠN");
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++;

        Row headerRow1 = sheet.createRow(rowNum++);
        String[] summaryHeaders = {"Chỉ tiêu", "Giá trị"};
        for (int i = 0; i < summaryHeaders.length; i++) {
            Cell cell = headerRow1.createCell(i);
            cell.setCellValue(summaryHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        int totalInvoices = allInvoices.size();
        double totalAmount = allInvoices.stream()
                .mapToDouble(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0.0)
                .sum();

        long paidCount = allInvoices.stream()
                .filter(inv -> inv.getStatus() != null && "PAID".equals(inv.getStatus().name()))
                .count();
        double paidAmount = allInvoices.stream()
                .filter(inv -> inv.getStatus() != null && "PAID".equals(inv.getStatus().name()))
                .mapToDouble(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0.0)
                .sum();

        long publishedCount = allInvoices.stream()
                .filter(inv -> inv.getStatus() != null && "PUBLISHED".equals(inv.getStatus().name()))
                .count();
        double publishedAmount = allInvoices.stream()
                .filter(inv -> inv.getStatus() != null && "PUBLISHED".equals(inv.getStatus().name()))
                .mapToDouble(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0.0)
                .sum();

        createSummaryRow(sheet, rowNum++, "Tổng số hóa đơn", String.valueOf(totalInvoices), dataStyle);
        createSummaryRow(sheet, rowNum++, "Tổng giá trị", totalAmount, currencyStyle);
        createSummaryRow(sheet, rowNum++, "Số hóa đơn đã thanh toán", String.valueOf(paidCount), dataStyle);
        createSummaryRow(sheet, rowNum++, "Tổng giá trị đã thanh toán", paidAmount, currencyStyle);
        createSummaryRow(sheet, rowNum++, "Số hóa đơn chưa thanh toán", String.valueOf(publishedCount), dataStyle);
        createSummaryRow(sheet, rowNum++, "Tổng giá trị chưa thanh toán", publishedAmount, currencyStyle);

        rowNum += 2;

        Row headerRow2 = sheet.createRow(rowNum++);
        String[] serviceHeaders = {"Loại dịch vụ", "Số lượng", "Tổng giá trị"};
        for (int i = 0; i < serviceHeaders.length; i++) {
            Cell cell = headerRow2.createCell(i);
            cell.setCellValue(serviceHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        for (Map.Entry<String, List<InvoiceDto>> entry : invoicesByService.entrySet()) {
            String serviceCode = entry.getKey();
            List<InvoiceDto> serviceInvoices = entry.getValue();
            List<InvoiceDto> uniqueInvoices = serviceInvoices.stream()
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            
            int count = uniqueInvoices.size();
            double amount = uniqueInvoices.stream()
                    .mapToDouble(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0.0)
                    .sum();

            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, getServiceSheetName(serviceCode), dataStyle);
            createCell(row, 1, String.valueOf(count), dataStyle);
            createCell(row, 2, amount, currencyStyle);
        }

        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, label, style);
        createCell(row, 1, value, style);
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, double value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, label, style);
        createCell(row, 1, value, style);
    }
}

