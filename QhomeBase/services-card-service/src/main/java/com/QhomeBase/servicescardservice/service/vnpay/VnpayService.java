package com.QhomeBase.servicescardservice.service.vnpay;

import com.QhomeBase.servicescardservice.config.VnpayProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayService {

    private final VnpayProperties properties;

    public String createPaymentUrl(Long orderId, String orderInfo, BigDecimal amountVnd, String clientIp) {
        return createPaymentUrlWithRef(orderId, orderInfo, amountVnd, clientIp, properties.getReturnUrl()).paymentUrl();
    }

    public String createPaymentUrl(Long orderId, String orderInfo, BigDecimal amountVnd, String clientIp, String returnUrl) {
        return createPaymentUrlWithRef(orderId, orderInfo, amountVnd, clientIp, returnUrl).paymentUrl();
    }

    public VnpayPaymentResult createPaymentUrlWithRef(Long orderId, String orderInfo, BigDecimal amountVnd, String clientIp, String returnUrl) {
        try {
            long amount = amountVnd.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", properties.getVersion());
            params.put("vnp_Command", properties.getCommand());
            params.put("vnp_TmnCode", properties.getTmnCode());
            params.put("vnp_Amount", String.valueOf(amount));
            params.put("vnp_CurrCode", "VND");

            String txnRef = orderId + "_" + System.currentTimeMillis();
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_OrderInfo", orderInfo);
            params.put("vnp_OrderType", "other");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", returnUrl);
            params.put("vnp_IpAddr", clientIp != null ? clientIp : "127.0.0.1");
            params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

            String hashData = buildHashData(params);
            String secureHash = hmacSHA512(properties.getHashSecret(), hashData);
            String query = buildQuery(params);

            String paymentUrl = properties.getVnpUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
            log.info("💳 [VNPAY] Tạo payment URL: orderId={}, amount={}, ip={}, txnRef={}", orderId, amountVnd, clientIp, txnRef);
            return new VnpayPaymentResult(paymentUrl, txnRef);
        } catch (Exception e) {
            log.error("❌ [VNPAY] Lỗi khi tạo URL thanh toán", e);
            throw new RuntimeException("Không thể tạo URL thanh toán VNPAY", e);
        }
    }

    public boolean validateReturn(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }

        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) {
            return false;
        }

        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        Map<String, String> sorted = new TreeMap<>(copy);
        String hashData = buildHashData(sorted);
        String calculated = hmacSHA512(properties.getHashSecret(), hashData);

        log.info("🔎 [VNPAY] Validate return: calculated={}, received={}", calculated, vnpSecureHash);
        return calculated.equalsIgnoreCase(vnpSecureHash) && "00".equals(params.get("vnp_ResponseCode"));
    }

    public Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String name = params.nextElement();
            String value = request.getParameter(name);
            if (value != null && !value.isBlank()) {
                fields.put(name, value);
            }
        }
        return fields;
    }

    private String buildHashData(Map<String, String> params) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                parts.add(entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                parts.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return String.join("&", parts);
    }

    private String buildQuery(Map<String, String> params) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                parts.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                parts.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return String.join("&", parts);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(spec);
            return DatatypeConverter.printHexBinary(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }
}


