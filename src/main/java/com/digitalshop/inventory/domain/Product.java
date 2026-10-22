package com.digitalshop.inventory.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** One row of the "product" table. */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Changed ONLY through ProductRepository's atomic update queries, never via setStock in a sale. */
    @Column(nullable = false)
    private int stock;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Product() { }

    public Product(String name, String category, String unit, BigDecimal price, int stock, int reorderLevel) {
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.price = price;
        this.stock = stock;
        this.reorderLevel = reorderLevel;
    }

    public boolean isOutOfStock() { return stock == 0; }
    public boolean isLowStock() { return stock > 0 && stock <= reorderLevel; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public Instant getCreatedAt() { return createdAt; }
}
