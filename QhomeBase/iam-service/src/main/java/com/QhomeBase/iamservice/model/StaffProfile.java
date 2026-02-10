package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "staff_profiles", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProfile {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "address")
    private String address;
}
