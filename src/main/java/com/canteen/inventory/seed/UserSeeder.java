package com.canteen.inventory.seed;

import com.canteen.inventory.config.AppProperties;
import com.canteen.inventory.domain.AppUser;
import com.canteen.inventory.domain.Role;
import com.canteen.inventory.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the two login accounts on first start. Passwords come from environment variables. */
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
        if (!users.existsByUsername(username)) {
            users.save(new AppUser(username, encoder.encode(rawPassword), role));
            log.info("Created user '{}' with role {}", username, role);
        }
    }
}
