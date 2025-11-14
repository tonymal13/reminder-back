package ru.mal.reminder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import ru.mal.reminder.client.KeycloakClient;
import ru.mal.reminder.dto.TokenResponse;
import ru.mal.reminder.dto.UserLoginRequest;
import ru.mal.reminder.dto.keycloak.KeycloakCredentialsRepresentation;
import ru.mal.reminder.dto.keycloak.KeycloakUserRepresentation;
import ru.mal.reminder.dto.registration.UserRegistrationRequest;
import ru.mal.reminder.model.User;

import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private MessageSource messageSource;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    private final String ADMIN_TOKEN = "admin-token";
    private final String USER_ID = "user-id-123";
    private final String SUCCESS_MESSAGE = "Registration successful";
    private final String ERROR_MESSAGE = "Registration error";

    @Test
    void registerUser_ShouldRegisterUserSuccessfully() {
        // Given
        UserRegistrationRequest request = createRegistrationRequest();
        TokenResponse adminTokenResponse = createTokenResponse(ADMIN_TOKEN);
        User user = createUser();

        Mockito.when(keycloakClient.adminLogin()).thenReturn(adminTokenResponse);
        Mockito.when(keycloakClient.registerUser(ArgumentMatchers.anyString(), ArgumentMatchers.any(KeycloakUserRepresentation.class)))
                .thenReturn(USER_ID);
        Mockito.when(userService.findOrCreateUser(USER_ID, request.getEmail(), request.getUsername()))
                .thenReturn(user);
        Mockito.when(messageSource.getMessage(ArgumentMatchers.eq("auth.register.success"), ArgumentMatchers.eq(null), ArgumentMatchers.any(Locale.class)))
                .thenReturn(SUCCESS_MESSAGE);

        // When
        String result = authService.registerUser(request);

        // Then
        Assertions.assertThat(result).isEqualTo(SUCCESS_MESSAGE);

        Mockito.verify(keycloakClient).adminLogin();
        Mockito.verify(keycloakClient).registerUser(ADMIN_TOKEN,
                new KeycloakUserRepresentation(
                        request.getUsername(),
                        request.getEmail(),
                        true,
                        false
                ));
        Mockito.verify(keycloakClient).resetUserPassword(ArgumentMatchers.eq(USER_ID), ArgumentMatchers.any(KeycloakCredentialsRepresentation.class), ArgumentMatchers.eq(ADMIN_TOKEN));
        Mockito.verify(keycloakClient).assignUserRole(USER_ID, "user", ADMIN_TOKEN);
        Mockito.verify(userService).findOrCreateUser(USER_ID, request.getEmail(), request.getUsername());
    }

    @Test
    void registerUser_ShouldThrowException_WhenKeycloakAdminLoginFails() {
        // Given
        UserRegistrationRequest request = createRegistrationRequest();

        Mockito.when(keycloakClient.adminLogin()).thenThrow(new RuntimeException("Keycloak unavailable"));
        Mockito.when(messageSource.getMessage(ArgumentMatchers.eq("auth.register.error"), ArgumentMatchers.eq(null), ArgumentMatchers.any(Locale.class)))
                .thenReturn(ERROR_MESSAGE);

        // When & Then
        Assertions.assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(ERROR_MESSAGE)
                .hasCauseInstanceOf(RuntimeException.class);

        Mockito.verify(keycloakClient).adminLogin();
        Mockito.verify(keycloakClient, Mockito.never()).registerUser(ArgumentMatchers.anyString(), ArgumentMatchers.any(KeycloakUserRepresentation.class));
        Mockito.verify(userService, Mockito.never()).findOrCreateUser(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
    }

    @Test
    void registerUser_ShouldThrowException_WhenUserRegistrationFails() {
        // Given
        UserRegistrationRequest request = createRegistrationRequest();
        TokenResponse adminTokenResponse = createTokenResponse(ADMIN_TOKEN);

        Mockito.when(keycloakClient.adminLogin()).thenReturn(adminTokenResponse);
        Mockito.when(keycloakClient.registerUser(ArgumentMatchers.anyString(), ArgumentMatchers.any(KeycloakUserRepresentation.class)))
                .thenThrow(new RuntimeException("Registration failed"));
        Mockito.when(messageSource.getMessage(ArgumentMatchers.eq("auth.register.error"), ArgumentMatchers.eq(null), ArgumentMatchers.any(Locale.class)))
                .thenReturn(ERROR_MESSAGE);

        // When & Then
        Assertions.assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(ERROR_MESSAGE)
                .hasCauseInstanceOf(RuntimeException.class);

        Mockito.verify(keycloakClient).adminLogin();
        Mockito.verify(keycloakClient).registerUser(ArgumentMatchers.anyString(), ArgumentMatchers.any(KeycloakUserRepresentation.class));
        Mockito.verify(keycloakClient, Mockito.never()).resetUserPassword(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.anyString());
        Mockito.verify(userService, Mockito.never()).findOrCreateUser(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
    }

    @Test
    void login_ShouldReturnTokenResponse_WhenCredentialsAreValid() {
        // Given
        UserLoginRequest request = new UserLoginRequest("testuser", "password");
        TokenResponse expectedToken = createTokenResponse("user-token");

        Mockito.when(keycloakClient.userLogin(request.getUsername(), request.getPassword()))
                .thenReturn(expectedToken);

        // When
        TokenResponse result = authService.login(request);

        // Then
        Assertions.assertThat(result).isEqualTo(expectedToken);
        Mockito.verify(keycloakClient).userLogin(request.getUsername(), request.getPassword());
    }

    @Test
    void login_ShouldThrowException_WhenCredentialsAreInvalid() {
        // Given
        UserLoginRequest request = new UserLoginRequest("testuser", "wrongpassword");
        String errorMessage = "Login error";

        Mockito.when(keycloakClient.userLogin(request.getUsername(), request.getPassword()))
                .thenThrow(new RuntimeException("Invalid credentials"));
        Mockito.when(messageSource.getMessage(ArgumentMatchers.eq("auth.login.error"), ArgumentMatchers.eq(null), ArgumentMatchers.any(Locale.class)))
                .thenReturn(errorMessage);

        // When & Then
        Assertions.assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage)
                .hasCauseInstanceOf(RuntimeException.class);

        Mockito.verify(keycloakClient).userLogin(request.getUsername(), request.getPassword());
    }

    private UserRegistrationRequest createRegistrationRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        return request;
    }

    private TokenResponse createTokenResponse(String accessToken) {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setExpiresIn(300);
        tokenResponse.setRefreshExpiresIn(1800);
        tokenResponse.setTokenType("Bearer");
        return tokenResponse;
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setKeycloakId(USER_ID);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        return user;
    }
}