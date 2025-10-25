package ru.mal.reminder.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import ru.mal.reminder.client.KeycloakClient;
import ru.mal.reminder.dto.TokenResponse;
import ru.mal.reminder.dto.UserLoginRequest;
import ru.mal.reminder.dto.keycloak.KeycloakCredentialsRepresentation;
import ru.mal.reminder.dto.keycloak.KeycloakUserRepresentation;
import ru.mal.reminder.dto.registration.UserRegistrationRequest;

import java.util.Locale;

import static ru.mal.reminder.Consts.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakClient keycloakClient;
    private final MessageSource messageSource;

    public String registerUser(UserRegistrationRequest request) {
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
                    GRANT_TYPE_PASSWORD,
                    request.getPassword(),
                    false
            );
            keycloakClient.resetUserPassword(userId, credentials, adminToken.getAccessToken());

            keycloakClient.assignUserRole(userId, USER_ROLE, adminToken.getAccessToken());

            return messageSource.getMessage("auth.register.success", null, Locale.getDefault());

        } catch (Exception e) {
            throw new RuntimeException(
                    messageSource.getMessage("auth.register.error", null, Locale.getDefault()),
                    e
            );
        }
    }

    public TokenResponse login(UserLoginRequest request) {
        try {
            return keycloakClient.userLogin(
                    request.getUsername(),
                    request.getPassword()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    messageSource.getMessage("auth.login.error", null, Locale.getDefault()),
                    e
            );
        }
    }
}