package com.QhomeBase.marketplaceservice.validation;

import com.QhomeBase.marketplaceservice.dto.ContactInfoRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContactInfoValidator implements ConstraintValidator<ValidContactInfo, ContactInfoRequest> {

    private static final String PHONE_PATTERN = "^[0-9]{10}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    @Override
    public void initialize(ValidContactInfo constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(ContactInfoRequest contactInfo, ConstraintValidatorContext context) {
        if (contactInfo == null) {
            return true; // Null is handled by @NotNull if needed
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        // Validate phone if showPhone is true
        if (Boolean.TRUE.equals(contactInfo.getShowPhone())) {
            if (contactInfo.getPhone() == null || contactInfo.getPhone().trim().isEmpty()) {
                context.buildConstraintViolationWithTemplate("Số điện thoại không được để trống khi chọn hiển thị số điện thoại")
                        .addPropertyNode("phone")
                        .addConstraintViolation();
                isValid = false;
            } else {
                String phone = contactInfo.getPhone().trim();
                // Remove all non-digit characters for validation
                String digitsOnly = phone.replaceAll("[^0-9]", "");
                if (digitsOnly.length() != 10) {
                    context.buildConstraintViolationWithTemplate("Số điện thoại phải có đúng 10 chữ số, không chứa ký tự đặc biệt hoặc khoảng trắng")
                            .addPropertyNode("phone")
                            .addConstraintViolation();
                    isValid = false;
                } else if (!digitsOnly.matches(PHONE_PATTERN)) {
                    context.buildConstraintViolationWithTemplate("Số điện thoại không hợp lệ")
                            .addPropertyNode("phone")
                            .addConstraintViolation();
                    isValid = false;
                }
            }
        }

        // Validate email if showEmail is true
        if (Boolean.TRUE.equals(contactInfo.getShowEmail())) {
            if (contactInfo.getEmail() == null || contactInfo.getEmail().trim().isEmpty()) {
                context.buildConstraintViolationWithTemplate("Email không được để trống khi chọn hiển thị email")
                        .addPropertyNode("email")
                        .addConstraintViolation();
                isValid = false;
            } else {
                String email = contactInfo.getEmail().trim();
                if (!email.matches(EMAIL_PATTERN)) {
                    context.buildConstraintViolationWithTemplate("Email không đúng định dạng")
                            .addPropertyNode("email")
                            .addConstraintViolation();
                    isValid = false;
                }
            }
        }

        return isValid;
    }
}

