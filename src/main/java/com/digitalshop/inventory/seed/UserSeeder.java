package com.digitalshop.inventory.seed;

import com.digitalshop.inventory.config.AppProperties;
import com.digitalshop.inventory.domain.AppUser;
import com.digitalshop.inventory.domain.Role;
import com.digitalshop.inventory.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Makes sure the two login accounts exist and their passwords match the environment variables
 * MANAGER_PASSWORD / STAFF_PASSWORD. Changing the variable in Render and redeploying changes the password.
 */
@Component
@Order(1)
public class UserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public UserSeeder(AppUserRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        create("manager", props.managerPassword(), Role.MANAGER);
        create("staff", props.staffPassword(), Role.STAFF);
    }

    private void create(String username, String rawPassword, Role role) {
        users.findByUsername(username).ifPresentOrElse(user -> {
            // BCrypt hashes can't be compared directly; matches() re-hashes the raw password with the stored salt
            if (!encoder.matches(rawPassword, user.getPasswordHash())) {
                user.setPasswordHash(encoder.encode(rawPassword));
                users.save(user);
                log.info("Password for user '{}' updated from environment variable", username);
            }
        }, () -> {
            users.save(new AppUser(username, encoder.encode(rawPassword), role));
            log.info("Created user '{}' with role {}", username, role);
        });
    }
}
