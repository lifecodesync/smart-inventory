package com.canteen.inventory.web;

import com.canteen.inventory.service.ConflictException;
import com.canteen.inventory.service.NotFoundException;
import com.canteen.inventory.service.ProductService;
import com.canteen.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** /sales/** = staff + manager. /purchases/** and /movements = manager only. */
@Controller
public class StockController {

    private final StockService stock;
    private final ProductService products;

    public StockController(StockService stock, ProductService products) {
        this.stock = stock;
        this.products = products;
    }

    @GetMapping("/sales/new")
    public String saleForm(Model model) {
        model.addAttribute("form", new MovementForm());
        return prepare(model, "sale");
    }

    @PostMapping("/sales")
    public String sell(@Valid @ModelAttribute("form") MovementForm form, BindingResult errors,
                       Authentication auth, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return prepare(model, "sale");
        }
        try {
            var sale = stock.recordSale(form.getProductId(), form.getQuantity(), auth.getName());
            flash.addFlashAttribute("success", "Sold " + sale.getQuantity() + " x " + sale.getProduct().getName()
                    + " = ₹" + sale.getTotal());
            return "redirect:/sales/new";
        } catch (ConflictException | NotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return prepare(model, "sale");
        }
    }

    @GetMapping("/purchases/new")
    public String purchaseForm(Model model) {
        model.addAttribute("form", new MovementForm());
        return prepare(model, "purchase");
    }

    @PostMapping("/purchases")
    public String purchase(@Valid @ModelAttribute("form") MovementForm form, BindingResult errors,
                           Authentication auth, Model model, RedirectAttributes flash) {
        if (form.getUnitCost() == null) {
            errors.rejectValue("unitCost", "required", "Cost per unit is required");
        }
        if (errors.hasErrors()) {
            return prepare(model, "purchase");
        }
        try {
            stock.recordPurchase(form.getProductId(), form.getQuantity(), form.getUnitCost(), auth.getName());
            flash.addFlashAttribute("success", "Added " + form.getQuantity() + " units to stock");
            return "redirect:/purchases/new";
        } catch (NotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return prepare(model, "purchase");
        }
    }

    @GetMapping("/movements")
    public String movements(Model model) {
        model.addAttribute("movements", stock.recent(100));
        return "stock/movements";
    }

    private String prepare(Model model, String mode) {
        model.addAttribute("mode", mode);
        model.addAttribute("products", products.search(null));
        model.addAttribute("recent", stock.recent(10));
        return "stock/form";
    }
}
