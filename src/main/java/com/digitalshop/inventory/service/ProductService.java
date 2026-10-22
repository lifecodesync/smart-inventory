package com.digitalshop.inventory.service;

import com.digitalshop.inventory.domain.Product;
import com.digitalshop.inventory.repository.ProductRepository;
import com.digitalshop.inventory.repository.StockMovementRepository;
import com.digitalshop.inventory.web.ProductForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository products;
    private final StockMovementRepository movements;

    // Dependency injection: Spring creates the repositories and hands them to us.
    public ProductService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    @Transactional(readOnly = true)
    public List<Product> search(String q) {
        if (q == null || q.isBlank()) {
            return products.findAllByOrderByNameAsc();
        }
        String term = q.trim();
        return products.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrderByNameAsc(term, term);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    @Transactional
    public Product create(ProductForm form) {
        String name = form.getName().trim();
        if (products.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A product named '" + name + "' already exists");
        }
        Product p = new Product(name, form.getCategory().trim(), form.getUnit().trim(),
                form.getPrice(), form.getStock(), form.getReorderLevel());
        return products.save(p);
    }

    /** Edits details. Stock is NOT edited here: it changes only via purchases and sales (audit trail). */
    @Transactional
    public Product update(Long id, ProductForm form) {
        Product p = get(id);
        String name = form.getName().trim();
        if (products.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("A product named '" + name + "' already exists");
        }
        p.setName(name);
        p.setCategory(form.getCategory().trim());
        p.setUnit(form.getUnit().trim());
        p.setPrice(form.getPrice());
        p.setReorderLevel(form.getReorderLevel());
        return p;   // dirty checking: JPA saves the changes when the transaction commits
    }

    @Transactional
    public void delete(Long id) {
        Product p = get(id);
        if (movements.existsByProductId(id)) {
            throw new ConflictException("'" + p.getName() + "' has sales/purchase history and cannot be deleted");
        }
        products.delete(p);
    }
}
