package ru.mal.reminder.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mal.reminder.client.KeycloakClient;
import ru.mal.reminder.dto.TokenResponse;
import ru.mal.reminder.dto.keycloak.KeycloakCredentialsRepresentation;
import ru.mal.reminder.dto.keycloak.KeycloakUserRepresentation;
import ru.mal.reminder.dto.registration.UserRegistrationRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserRegistrationController {

    private final KeycloakClient keycloakClient;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request) {
        try {
            TokenResponse adminToken = keycloakClient.adminLogin();

            KeycloakUserRepresentation user = new KeycloakUserRepresentation(
                    request.getUsername(),
                    request.getEmail(),
                    true,
                    false
            );

            String userId = keycloakClient.registerUser(adminToken.getAccessToken(), user);

            KeycloakCredentialsRepresentation credentials = new KeycloakCredentialsRepresentation(
                    "password",
                    request.getPassword(),
                    false
            );
            keycloakClient.resetUserPassword(userId, credentials, adminToken.getAccessToken());

            keycloakClient.assignUserRole(userId, "user", adminToken.getAccessToken());

            return ResponseEntity.ok("User registered successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}