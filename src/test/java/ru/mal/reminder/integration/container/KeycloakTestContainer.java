package ru.mal.reminder.integration.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import ru.mal.reminder.integration.config.Setting;

import java.time.Duration;

public class KeycloakTestContainer {

    public static final GenericContainer<?> keycloakTestContainer;

    static {
        keycloakTestContainer = new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:21.0.0"))
                .dependsOn(Containers.postgres)
                .withExposedPorts(8080)
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
                .withEnv("KC_DB", "postgres")
                .withEnv("KC_DB_URL", "jdbc:postgresql://postgres:5432/reminder-app-db")
                .withEnv("KC_DB_USERNAME", "postgres")
                .withEnv("KC_DB_PASSWORD", "postgres")
                .withCommand("start-dev --import-realm --health-enabled=true")
                .withNetwork(Setting.GLOBAL_NETWORK)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(Setting.realmInitPath()),
                        "/opt/keycloak/data/import/realm-config.json"
                )
                .waitingFor(Wait.forHttp("/"))
                .withStartupTimeout(Duration.ofMinutes(2));
    }
}