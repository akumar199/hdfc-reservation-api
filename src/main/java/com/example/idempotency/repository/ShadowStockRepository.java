package com.example.idempotency.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.idempotency.entity.ShadowStock;

public interface ShadowStockRepository
    extends JpaRepository<ShadowStock, Long> {

Optional<ShadowStock> findByItemId(String itemId);

}
