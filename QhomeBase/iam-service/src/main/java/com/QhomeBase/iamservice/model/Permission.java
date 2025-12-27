package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description")
    private String description;

    public boolean isBaseService() {
        return this.code.startsWith("base.");
    }

    public boolean isIamService() {
        return this.code.startsWith("iam.");
    }

    public boolean isMaintenanceService() {
        return this.code.startsWith("maintenance.");
    }

    public boolean isFinanceService() {
        return this.code.startsWith("finance.");
    }

    public boolean isDocumentService() {
        return this.code.startsWith("document.");
    }

    public boolean isReportService() {
        return this.code.startsWith("report.");
    }

    public boolean isSystemService() {
        return this.code.startsWith("system.");
    }
}









































