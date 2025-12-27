package com.QhomeBase.baseservice.websocket;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MoneyBillDto(
    BigDecimal money,
    BigDecimal consumption,
    int month,
    int year
) {}
