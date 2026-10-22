package com.digitalshop.inventory.service;

import com.digitalshop.inventory.domain.MovementType;
import com.digitalshop.inventory.domain.Product;
import com.digitalshop.inventory.domain.StockMovement;
import com.digitalshop.inventory.repository.ProductRepository;
import com.digitalshop.inventory.repository.StockMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public StockService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    /**
     * Record a sale. @Transactional = all-or-nothing, like a shop bill:
     * either stock goes down AND the sale is written, or neither happens.
     */
    @Transactional
    public StockMovement recordSale(Long productId, int quantity, String username) {
        requirePositive(quantity);
        int updated = products.decrementStock(productId, quantity);   // atomic check-and-subtract
        Product p = products.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " not found"));
        if (updated == 0) {
            log.info("Sale refused: {} x{} by {} (stock {})", p.getName(), quantity, username, p.getStock());
            throw new InsufficientStockException(p.getName(), quantity, p.getStock());
        }
        StockMovement sale = new StockMovement(p, MovementType.SALE, quantity, p.getPrice(), username, Instant.now());
        log.info("Sale: {} x{} by {}", p.getName(), quantity, username);
        return movements.save(sale);
    }

    /** Record a purchase (stock in) at the cost price paid to the supplier. */
    @Transactional
    public StockMovement recordPurchase(Long productId, int quantity, BigDecimal unitCost, String username) {
        requirePositive(quantity);
        if (unitCost == null || unitCost.signum() < 0) {
            throw new IllegalArgumentException("Unit cost must be 0 or more");
        }
        int updated = products.incrementStock(productId, quantity);
        if (updated == 0) {
            throw new NotFoundException("Product " + productId + " not found");
        }
        Product p = products.getReferenceById(productId);
        log.info("Purchase: product {} x{} by {}", productId, quantity, username);
        return movements.save(new StockMovement(p, MovementType.PURCHASE, quantity, unitCost, username, Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<StockMovement> recent(int limit) {
        return movements.findRecent(PageRequest.of(0, limit));
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
    }
}
