package com.Vencedora.NairaWave.service;

import com.Vencedora.NairaWave.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.Set;

public final class PaymentRules {

    public static final Set<String> CATEGORIES = Set.of(
            "airtime", "data", "electricity", "transfer"
    );

    private PaymentRules() {}

    public static void validate(String category, BigDecimal amount) {
        if (category == null || !CATEGORIES.contains(category.toLowerCase())) {
            throw new BadRequestException("Unsupported payment category");
        }
        String cat = category.toLowerCase();
        if (cat.equals("airtime") && amount.compareTo(new BigDecimal("50")) < 0) {
            throw new BadRequestException("Minimum airtime is ₦50");
        }
        if (cat.equals("data") && amount.compareTo(new BigDecimal("100")) < 0) {
            throw new BadRequestException("Minimum data purchase is ₦100");
        }
        if (cat.equals("electricity") && amount.compareTo(new BigDecimal("500")) < 0) {
            throw new BadRequestException("Minimum electricity payment is ₦500");
        }
        if (cat.equals("transfer") && amount.compareTo(new BigDecimal("100")) < 0) {
            throw new BadRequestException("Minimum transfer is ₦100");
        }
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new BadRequestException("Amount exceeds single-transaction limit");
        }
    }
}