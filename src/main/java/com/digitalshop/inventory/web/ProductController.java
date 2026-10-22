package com.digitalshop.inventory.web;

import com.digitalshop.inventory.service.ConflictException;
import com.digitalshop.inventory.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Manager-only (see SecurityConfig): list, search, add, edit, delete products. */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("products", service.search(q));
        model.addAttribute("q", q);
        return "products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ProductForm());
        model.addAttribute("editId", null);
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult errors,
                         Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("editId", null);
            return "products/form";             // show the same form again with error messages
        }
        try {
            var p = service.create(form);
            flash.addFlashAttribute("success", "Added " + p.getName());
            return "redirect:/products";        // Post/Redirect/Get: refresh won't resubmit
        } catch (ConflictException e) {
            errors.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("editId", null);
            return "products/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", ProductForm.from(service.get(id)));
        model.addAttribute("editId", id);
        return "products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form,
                         BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("editId", id);
            return "products/form";
        }
        try {
            var p = service.update(id, form);
            flash.addFlashAttribute("success", "Updated " + p.getName());
            return "redirect:/products";
        } catch (ConflictException e) {
            errors.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("editId", id);
            return "products/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            service.delete(id);
            flash.addFlashAttribute("success", "Product deleted");
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products";
    }
}
