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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        when(keycloakClient.adminLogin()).thenReturn(adminTokenResponse);
        when(keycloakClient.registerUser(anyString(), any(KeycloakUserRepresentation.class)))
                .thenReturn(USER_ID);
        when(userService.findOrCreateUser(USER_ID, request.getEmail(), request.getUsername()))
                .thenReturn(user);
        when(messageSource.getMessage(eq("auth.register.success"), eq(null), any(Locale.class)))
                .thenReturn(SUCCESS_MESSAGE);

        // When
        String result = authService.registerUser(request);

        // Then
        assertThat(result).isEqualTo(SUCCESS_MESSAGE);

        verify(keycloakClient).adminLogin();
        verify(keycloakClient).registerUser(ADMIN_TOKEN,
                new KeycloakUserRepresentation(
                        request.getUsername(),
                        request.getEmail(),
                        true,
                        false
                ));
        verify(keycloakClient).resetUserPassword(eq(USER_ID), any(KeycloakCredentialsRepresentation.class), eq(ADMIN_TOKEN));
        verify(keycloakClient).assignUserRole(USER_ID, "user", ADMIN_TOKEN);
        verify(userService).findOrCreateUser(USER_ID, request.getEmail(), request.getUsername());
    }

    @Test
    void registerUser_ShouldThrowException_WhenKeycloakAdminLoginFails() {
        // Given
        UserRegistrationRequest request = createRegistrationRequest();

        when(keycloakClient.adminLogin()).thenThrow(new RuntimeException("Keycloak unavailable"));
        when(messageSource.getMessage(eq("auth.register.error"), eq(null), any(Locale.class)))
                .thenReturn(ERROR_MESSAGE);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(ERROR_MESSAGE)
                .hasCauseInstanceOf(RuntimeException.class);

        verify(keycloakClient).adminLogin();
        verify(keycloakClient, never()).registerUser(anyString(), any(KeycloakUserRepresentation.class));
        verify(userService, never()).findOrCreateUser(anyString(), anyString(), anyString());
    }

    @Test
    void registerUser_ShouldThrowException_WhenUserRegistrationFails() {
        // Given
        UserRegistrationRequest request = createRegistrationRequest();
        TokenResponse adminTokenResponse = createTokenResponse(ADMIN_TOKEN);

        when(keycloakClient.adminLogin()).thenReturn(adminTokenResponse);
        when(keycloakClient.registerUser(anyString(), any(KeycloakUserRepresentation.class)))
                .thenThrow(new RuntimeException("Registration failed"));
        when(messageSource.getMessage(eq("auth.register.error"), eq(null), any(Locale.class)))
                .thenReturn(ERROR_MESSAGE);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(ERROR_MESSAGE)
                .hasCauseInstanceOf(RuntimeException.class);

        verify(keycloakClient).adminLogin();
        verify(keycloakClient).registerUser(anyString(), any(KeycloakUserRepresentation.class));
        verify(keycloakClient, never()).resetUserPassword(anyString(), any(), anyString());
        verify(userService, never()).findOrCreateUser(anyString(), anyString(), anyString());
    }

    @Test
    void login_ShouldReturnTokenResponse_WhenCredentialsAreValid() {
        // Given
        UserLoginRequest request = new UserLoginRequest("testuser", "password");
        TokenResponse expectedToken = createTokenResponse("user-token");

        when(keycloakClient.userLogin(request.getUsername(), request.getPassword()))
                .thenReturn(expectedToken);

        // When
        TokenResponse result = authService.login(request);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(keycloakClient).userLogin(request.getUsername(), request.getPassword());
    }

    @Test
    void login_ShouldThrowException_WhenCredentialsAreInvalid() {
        // Given
        UserLoginRequest request = new UserLoginRequest("testuser", "wrongpassword");
        String errorMessage = "Login error";

        when(keycloakClient.userLogin(request.getUsername(), request.getPassword()))
                .thenThrow(new RuntimeException("Invalid credentials"));
        when(messageSource.getMessage(eq("auth.login.error"), eq(null), any(Locale.class)))
                .thenReturn(errorMessage);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage)
                .hasCauseInstanceOf(RuntimeException.class);

        verify(keycloakClient).userLogin(request.getUsername(), request.getPassword());
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