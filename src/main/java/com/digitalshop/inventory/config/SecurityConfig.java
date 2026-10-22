package com.digitalshop.inventory.config;

import com.digitalshop.inventory.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
public class SecurityConfig {

    /**
     * Rules are checked top to bottom; the first match wins.
     * Staff: sales only. Manager: everything.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/favicon.ico", "/error", "/actuator/health/**").permitAll()
                .requestMatchers("/", "/sales/**", "/403").hasAnyRole("MANAGER", "STAFF")
                .anyRequest().hasRole("MANAGER"))
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    /**
     * Login form with an expired CSRF token (e.g. the page was open while the server restarted)
     * -> back to the login page with a friendly message. Every other denial -> our 403 page.
     */
    private AccessDeniedHandler accessDeniedHandler() {
        AccessDeniedHandlerImpl toForbiddenPage = new AccessDeniedHandlerImpl();
        toForbiddenPage.setErrorPage("/403");
        return (request, response, denied) -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (denied instanceof CsrfException && "/login".equals(path)) {
                response.sendRedirect(request.getContextPath() + "/login?expired");
            } else {
                toForbiddenPage.handle(request, response, denied);
            }
        };
    }

    /** BCrypt = slow, salted, one-way password hashing. Strength 10 ≈ 1024 rounds. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** How Spring Security finds a user at login: look up our app_user table. */
    @Bean
    public UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No user " + username));
    }
}
