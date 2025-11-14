package ru.mal.reminder.integration.container;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestContainer {

    public static final PostgreSQLContainer<?> postgresTestContainer;

    static {
        postgresTestContainer = new PostgreSQLContainer<>("postgres:13")
                .withDatabaseName("reminder-app-db")
                .withUsername("postgres")
                .withPassword("postgres")
                .withNetwork(ru.mal.reminder.integration.config.Setting.GLOBAL_NETWORK)
                .withNetworkAliases("postgres");
    }
}