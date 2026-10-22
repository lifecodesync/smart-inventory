package com.digitalshop.inventory.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    /** After login: manager goes to the dashboard, staff goes straight to the sale counter. */
    @GetMapping("/")
    public String home(Authentication auth) {
        boolean manager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        return manager ? "redirect:/dashboard" : "redirect:/sales/new";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Any method (GET or POST): Spring Security forwards the ORIGINAL request here, which may be a POST. */
    @RequestMapping("/403")
    public String forbidden() {
        return "403";
    }
}
