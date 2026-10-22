package com.canteen.inventory.repository;

import com.canteen.inventory.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByNameAsc();

    List<Product> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrderByNameAsc(String name, String category);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * THE anti-overselling query.
     * Check ("is there enough?") and change ("subtract") happen in ONE SQL statement.
     * PostgreSQL locks the row while updating, so two staff selling the last samosa
     * cannot both pass the check: the second one sees stock = 0 and updates 0 rows.
     *
     * @return number of rows changed: 1 = sold, 0 = not enough stock (or no such product)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
}
