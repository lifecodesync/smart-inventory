package com.digitalshop.inventory.repository;

import com.digitalshop.inventory.domain.StockMovement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /** "join fetch" loads the product in the same query, so the page can show its name. */
    @Query("select m from StockMovement m join fetch m.product order by m.createdAt desc, m.id desc")
    List<StockMovement> findRecent(Pageable pageable);

    boolean existsByProductId(Long productId);
}
