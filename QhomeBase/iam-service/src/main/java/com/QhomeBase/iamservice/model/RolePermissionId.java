package com.QhomeBase.iamservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RolePermissionId implements Serializable {

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "permission_code", nullable = false)
    private String permissionCode;
}