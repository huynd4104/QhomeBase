package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for resending a rejected household member request.
 * All fields are optional - if not provided, values from the original request will be used.
 */
public record HouseholdMemberRequestResendDto(
        String residentFullName,

        @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải có đúng 10 số, bắt đầu từ số 0, không được có dấu cách hoặc ký tự đặc biệt")
        String residentPhone,

        String residentEmail,

        @Pattern(regexp = "^[0-9]{12}$", message = "CCCD phải có đúng 12 chữ số, không được có dấu cách hoặc ký tự đặc biệt")
        String residentNationalId,

        LocalDate residentDob,

        String relation,

        String proofOfRelationImageUrl,

        String note
) {}
