package com.example.idempotency.entity;



import jakarta.persistence.*;

@Entity
@Table(name = "shadow_stock")
public class ShadowStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false, unique = true)
    private String itemId;

    @Column(nullable = false)
    private Integer available;

    public ShadowStock() {
    }

    public Long getId() {
        return id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Integer getAvailable() {
        return available;
    }

    public void setAvailable(Integer available) {
        this.available = available;
    }
}