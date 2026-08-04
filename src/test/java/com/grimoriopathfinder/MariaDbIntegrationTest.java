package com.grimoriopathfinder;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MariaDBContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class MariaDbIntegrationTest {
    static { System.setProperty("testcontainers.ryuk.disabled", "true"); System.setProperty("api.version", "1.44"); }
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.4").withDatabaseName("gestion_grimorio").withUsername("gestion_app").withPassword("test-password");
    @BeforeAll static void startContainer() { MARIADB.start(); }
    @DynamicPropertySource static void databaseProperties(DynamicPropertyRegistry r) { r.add("spring.datasource.url", MARIADB::getJdbcUrl); r.add("spring.datasource.username", MARIADB::getUsername); r.add("spring.datasource.password", MARIADB::getPassword); }
}
