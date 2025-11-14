package ru.mal.reminder.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mal.reminder.ReminderApplication;
import ru.mal.reminder.integration.container.Containers;
import ru.mal.reminder.integration.container.KeycloakTestContainer;
import ru.mal.reminder.integration.service.AuthTestService;
import ru.mal.reminder.integration.service.KeycloakTestService;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {ReminderApplication.class}
)
@ActiveProfiles("test")
public abstract class LifecycleSpecification {

    @Autowired
    protected AuthTestService authTestService;

    @Autowired
    protected KeycloakTestService keycloakTestService;

    static {
        Containers.run();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        String keycloakBase = "http://" +
                KeycloakTestContainer.keycloakTestContainer.getHost() + ":" +
                KeycloakTestContainer.keycloakTestContainer.getFirstMappedPort();

        String postgresUrl = Containers.postgres.getJdbcUrl();

        registry.add("spring.datasource.url", () -> postgresUrl);
        registry.add("spring.datasource.username", Containers.postgres::getUsername);
        registry.add("spring.datasource.password", Containers.postgres::getPassword);

        registry.add("keycloak.server-url", () -> keycloakBase);
        registry.add("keycloak.realm", () -> "reminderapp");
        registry.add("keycloak.client-id", () -> "reminder-app");
        registry.add("keycloak.client-secret", () -> "reminder-app-secret");
        registry.add("keycloak.admin-client-id", () -> "admin-cli");
        registry.add("keycloak.admin-username", () -> "admin");
        registry.add("keycloak.admin-password", () -> "admin");

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakBase + "/realms/reminderapp");
    }
}