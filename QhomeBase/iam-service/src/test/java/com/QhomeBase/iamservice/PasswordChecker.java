package com.QhomeBase.iamservice;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordChecker {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "abc12345";

        // Băm lần 1
        String encodedPassword1 = encoder.encode(rawPassword);
        // Băm lần 2 (để thấy sự khác biệt)
        String encodedPassword2 = encoder.encode(rawPassword);

        System.out.println("--- KẾT QUẢ KIỂM TRA BĂM MẬT KHẨU ---");
        System.out.println("Mật khẩu gốc: " + rawPassword);
        System.out.println("Kết quả băm lần 1: " + encodedPassword1);
        System.out.println("Kết quả băm lần 2: " + encodedPassword2);

        // Kiểm tra tính hợp lệ
        boolean isMatch = encoder.matches(rawPassword, encodedPassword1);
        System.out.println("Kiểm tra khớp mật khẩu: " + (isMatch ? "THÀNH CÔNG" : "THẤT BẠI"));
    }
}
