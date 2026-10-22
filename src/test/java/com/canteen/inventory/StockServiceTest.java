package com.canteen.inventory;

import com.canteen.inventory.domain.Product;
import com.canteen.inventory.repository.ProductRepository;
import com.canteen.inventory.service.InsufficientStockException;
import com.canteen.inventory.service.NotFoundException;
import com.canteen.inventory.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockServiceTest extends DatabaseTest {

    @Autowired StockService stock;
    @Autowired ProductRepository products;

    private Product tea(int qty) {
        return products.save(new Product("Tea", "Beverages", "cup", new BigDecimal("10"), qty, 5));
    }

    @Test
    void saleReducesStockAndWritesALedgerRow() {
        Product p = tea(10);
        stock.recordSale(p.getId(), 3, "staff");
        assertThat(products.findById(p.getId()).orElseThrow().getStock()).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT SUM(quantity) FROM stock_movement WHERE type='SALE'", Integer.class)).isEqualTo(3);
    }

    @Test
    void sellingMoreThanAvailableIsRefusedAndNothingChanges() {
        Product p = tea(2);
        assertThatThrownBy(() -> stock.recordSale(p.getId(), 5, "staff"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("only 2 left");
        assertThat(products.findById(p.getId()).orElseThrow().getStock()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movement", Integer.class)).isZero();
    }

    @Test
    void purchaseIncreasesStock() {
        Product p = tea(2);
        stock.recordPurchase(p.getId(), 50, new BigDecimal("4"), "manager");
        assertThat(products.findById(p.getId()).orElseThrow().getStock()).isEqualTo(52);
    }

    @Test
    void sellingUnknownProductIs404() {
        assertThatThrownBy(() -> stock.recordSale(999L, 1, "staff")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void zeroOrNegativeQuantityIsRejected() {
        Product p = tea(5);
        assertThatThrownBy(() -> stock.recordSale(p.getId(), 0, "staff")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.recordSale(p.getId(), -3, "staff")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void databaseCheckRuleBlocksNegativeStockEvenIfJavaCodeIsBypassed() {
        Product p = tea(1);
        assertThatThrownBy(() -> jdbc.update("UPDATE product SET stock = -1 WHERE id = ?", p.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
