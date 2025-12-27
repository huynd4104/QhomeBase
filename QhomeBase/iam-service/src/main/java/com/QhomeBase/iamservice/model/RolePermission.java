package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="role_permissions", schema="iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

   @EmbeddedId
   private RolePermissionId rolePermissionId;
}