package com.digitalshop.inventory;

import com.digitalshop.inventory.domain.Product;
import com.digitalshop.inventory.repository.ProductRepository;
import com.digitalshop.inventory.service.InsufficientStockException;
import com.digitalshop.inventory.service.StockService;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * THE interview test: 10 counter staff try to sell the LAST pair of earbuds at the same instant.
 * Exactly one must succeed; stock must end at 0, never -9.
 */
class ConcurrentSaleTest extends DatabaseTest {

    @Autowired StockService stock;
    @Autowired ProductRepository products;

    @RepeatedTest(5)   // run it 5 times: race bugs often hide on the first try
    void onlyOneOfManySimultaneousSalesOfTheLastItemSucceeds() throws Exception {
        Product earbuds = products.save(new Product("Wireless Earbuds", "Audio", "piece", new BigDecimal("1999"), 1, 5));
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger sold = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            String staff = "staff" + i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();                      // everyone waits here, then all start together
                try {
                    stock.recordSale(earbuds.getId(), 1, staff);
                    sold.incrementAndGet();
                } catch (InsufficientStockException e) {
                    refused.incrementAndGet();
                }
                return null;
            }));
        }
        ready.await();
        go.countDown();                           // GO!
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(sold.get()).isEqualTo(1);
        assertThat(refused.get()).isEqualTo(threads - 1);
        assertThat(products.findById(earbuds.getId()).orElseThrow().getStock()).isZero();
        Integer saleRows = jdbc.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE type = 'SALE'", Integer.class);
        assertThat(saleRows).isEqualTo(1);
    }
}
