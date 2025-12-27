package com.QhomeBase.baseservice.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MoneyBillWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    // Gửi thông báo cập nhật tiền điện nước cho resident/app
    public void sendMoneyBill(UUID residentId, BigDecimal money, BigDecimal consumption, int month, int year) {
        MoneyBillDto dto = new MoneyBillDto(money, consumption, month, year);
        messagingTemplate.convertAndSend("/topic/money-used/" + residentId, dto);
    }

    // Có thể tạo method gọi hàm này khi tổng hợp readings hoặc khi phiếu nhập báo thành công
    // Ví dụ:
    // public void notifyBillForCurrentMonth(UUID residentId, BigDecimal money, BigDecimal consumption) {
    //     int nowMonth = LocalDate.now().getMonthValue();
    //     int nowYear = LocalDate.now().getYear();
    //     sendMoneyBill(residentId, money, consumption, nowMonth, nowYear);
    // }
}
