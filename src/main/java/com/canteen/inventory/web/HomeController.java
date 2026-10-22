package com.canteen.inventory.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping("/403")
    public String forbidden() {
        return "403";
    }
}
