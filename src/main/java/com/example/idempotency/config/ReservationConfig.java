package com.example.idempotency.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReservationConfig {

    @Value("${standin.max-per-item:10}")
    private int standinMaxPerItem;

    @Value("${authority.url}")
    private String authorityUrl;

    public int getStandinMaxPerItem() {
        return standinMaxPerItem;
    }

    public String getAuthorityUrl() {
        return authorityUrl;
    }
}