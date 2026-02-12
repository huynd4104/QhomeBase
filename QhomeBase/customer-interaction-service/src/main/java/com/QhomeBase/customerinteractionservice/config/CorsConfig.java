package com.QhomeBase.customerinteractionservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 1. Cho phép gửi thông tin credentials (cookie, session...)
        config.setAllowCredentials(true);

        // 2. SỬA LẠI Ở ĐÂY:
        // Phải chỉ định chính xác domain của frontend.
        // KHÔNG được dùng "*" khi setAllowCredentials(true).
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://127.0.0.1:3000");
        config.addAllowedOrigin("http://localhost:3001");
        // (Nếu frontend chạy ở port khác, hãy thay 3000 bằng port đó)
        // (Nếu deploy lên production, bạn thêm domain thật:
        // config.addAllowedOrigin("https://app.qhomebase.com"))

        // 3. Các cấu hình khác
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Authorization"); // (Đúng, để frontend đọc được JWT)

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsFilter(corsConfigurationSource);
    }
}