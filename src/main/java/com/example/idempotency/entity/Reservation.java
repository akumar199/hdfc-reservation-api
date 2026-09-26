package com.example.idempotency.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "reservations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_idempotency_key",
            columnNames = {"user_id", "idempotency_key"}
        )
    }
)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(nullable = false)
    private Integer qty;

    @Column(nullable = false)
    private String status;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    public Reservation() {
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}