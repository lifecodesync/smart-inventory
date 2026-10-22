package com.canteen.inventory;

import com.canteen.inventory.domain.Product;
import com.canteen.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MockMvc = a fake browser that sends requests to the app without opening a real port. */
@AutoConfigureMockMvc
class SecurityAndWebTest extends DatabaseTest {

    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;

    // ---------- login ----------

    @Test
    void correctPasswordLogsIn() throws Exception {
        mvc.perform(formLogin("/login").user("manager").password("Manager@123"))
                .andExpect(authenticated().withRoles("MANAGER"));
    }

    @Test
    void wrongPasswordFails() throws Exception {
        mvc.perform(formLogin("/login").user("manager").password("wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void passwordsAreStoredAsBcryptHashesNotPlainText() {
        String hash = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username = 'staff'", String.class);
        org.assertj.core.api.Assertions.assertThat(hash).startsWith("$2").doesNotContain("Staff@123");
    }

    @Test
    void anonymousUserIsSentToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------- roles ----------

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void staffCanOpenSalePage() throws Exception {
        mvc.perform(get("/sales/new")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void staffIsBlockedFromManagerPages() throws Exception {
        for (String url : new String[]{"/dashboard", "/products", "/products/new", "/purchases/new", "/forecast", "/alerts"}) {
            mvc.perform(get(url)).andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerCanOpenEverything() throws Exception {
        for (String url : new String[]{"/dashboard", "/products", "/products/new", "/sales/new", "/purchases/new",
                "/movements", "/forecast", "/alerts"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    // ---------- validation ----------

    @Test
    @WithMockUser(roles = "MANAGER")
    void validProductIsSaved() throws Exception {
        mvc.perform(post("/products").with(csrf())
                        .param("name", "Poha").param("category", "Breakfast").param("unit", "plate")
                        .param("price", "25").param("stock", "40").param("reorderLevel", "10"))
                .andExpect(redirectedUrl("/products"));
        org.assertj.core.api.Assertions.assertThat(products.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void emptyNameAndNegativePriceAreRejected() throws Exception {
        mvc.perform(post("/products").with(csrf())
                        .param("name", "  ").param("category", "Snacks").param("unit", "piece")
                        .param("price", "-5").param("stock", "10").param("reorderLevel", "2"))
                .andExpect(status().isOk())                        // form shown again, not saved
                .andExpect(model().attributeHasFieldErrors("form", "name", "price"));
        org.assertj.core.api.Assertions.assertThat(products.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void duplicateNameIsRejected() throws Exception {
        products.save(new Product("Tea", "Beverages", "cup", new BigDecimal("10"), 5, 2));
        mvc.perform(post("/products").with(csrf())
                        .param("name", "tea").param("category", "Beverages").param("unit", "cup")
                        .param("price", "10").param("stock", "1").param("reorderLevel", "1"))
                .andExpect(model().attributeHasFieldErrors("form", "name"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void editingMissingProductIs404() throws Exception {
        mvc.perform(get("/products/9999/edit")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/products").param("name", "X")).andExpect(status().isForbidden());
    }

    // ---------- sale through the web form ----------

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void staffSaleThroughFormReducesStock() throws Exception {
        Product p = products.save(new Product("Tea", "Beverages", "cup", new BigDecimal("10"), 5, 2));
        mvc.perform(post("/sales").with(csrf()).param("productId", p.getId().toString()).param("quantity", "2"))
                .andExpect(redirectedUrl("/sales/new"));
        org.assertj.core.api.Assertions.assertThat(products.findById(p.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void overSaleThroughFormShowsErrorMessage() throws Exception {
        Product p = products.save(new Product("Tea", "Beverages", "cup", new BigDecimal("10"), 1, 2));
        mvc.perform(post("/sales").with(csrf()).param("productId", p.getId().toString()).param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", org.hamcrest.Matchers.containsString("only 1 left")));
    }
}
