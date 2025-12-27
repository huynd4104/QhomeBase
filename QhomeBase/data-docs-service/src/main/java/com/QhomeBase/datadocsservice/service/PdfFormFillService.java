package com.QhomeBase.datadocsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfFormFillService {

    public byte[] fillTemplate(String templateClasspathPath, Map<String, String> data, boolean flatten) {
        try (InputStream in = new ClassPathResource(templateClasspathPath).getInputStream();
             PDDocument doc = PDDocument.load(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            if (form == null) {
                throw new IllegalArgumentException("PDF template không có AcroForm");
            }

            for (Map.Entry<String, String> entry : data.entrySet()) {
                String fieldName = entry.getKey();
                String value = entry.getValue() == null ? "" : entry.getValue();
                PDField field = form.getField(fieldName);
                if (field != null) {
                    field.setValue(value);
                } else {
                    log.warn("Không tìm thấy field trong PDF: {}", fieldName);
                }
            }

            if (flatten) {
                form.flatten();
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo PDF: " + e.getMessage(), e);
        }
    }
}


