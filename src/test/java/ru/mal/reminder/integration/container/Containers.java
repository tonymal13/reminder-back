package ru.mal.reminder.integration.container;

import lombok.experimental.UtilityClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@UtilityClass
public class Containers {

    public static PostgreSQLContainer<?> postgres = PostgresTestContainer.postgresTestContainer;
    public static GenericContainer<?> keycloak = KeycloakTestContainer.keycloakTestContainer;

    public static void run() {
        postgres.start();
        keycloak.start();
    }
}