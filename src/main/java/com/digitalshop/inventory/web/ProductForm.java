package com.digitalshop.inventory.web;

import com.digitalshop.inventory.domain.Product;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * What the "add / edit product" form sends. Validation rules live here:
 * NEVER trust user input — the browser can send anything.
 */
public class ProductForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Category is required")
    @Size(max = 50)
    private String category;

    @NotBlank(message = "Unit is required (e.g. piece, box, pack)")
    @Size(max = 20)
    private String unit;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Price can have at most 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Opening stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Max(value = 100000, message = "Stock looks too large")
    private Integer stock = 0;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel = 10;

    public static ProductForm from(Product p) {
        ProductForm f = new ProductForm();
        f.name = p.getName();
        f.category = p.getCategory();
        f.unit = p.getUnit();
        f.price = p.getPrice();
        f.stock = p.getStock();
        f.reorderLevel = p.getReorderLevel();
        return f;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }
}
