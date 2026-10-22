package com.digitalshop.inventory.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** One line in the stock ledger: who moved how much of what, when. */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockMovement() { }

    public StockMovement(Product product, MovementType type, int quantity, BigDecimal unitPrice,
                         String createdBy, Instant createdAt) {
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public BigDecimal getTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public MovementType getType() { return type; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
