package com.digitalshop.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Base class: starts the real app against the test database and empties the tables before each test. */
@SpringBootTest
@ActiveProfiles("test")
public abstract class DatabaseTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE stock_movement, product RESTART IDENTITY CASCADE");
    }
}
