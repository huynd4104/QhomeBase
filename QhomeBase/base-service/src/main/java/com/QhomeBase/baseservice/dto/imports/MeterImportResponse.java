package com.QhomeBase.baseservice.dto.imports;

import java.util.ArrayList;
import java.util.List;

public class MeterImportResponse {
    private int totalRows;
    private int successCount;
    private int errorCount;
    private final List<MeterImportRowResult> rows = new ArrayList<>();

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public List<MeterImportRowResult> getRows() {
        return rows;
    }
}


