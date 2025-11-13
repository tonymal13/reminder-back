package ru.mal.reminder.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.mal.reminder.config.KeycloakProperties;
import ru.mal.reminder.dto.TokenResponse;
import ru.mal.reminder.dto.keycloak.KeycloakCredentialsRepresentation;
import ru.mal.reminder.dto.keycloak.KeycloakUserRepresentation;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KeycloakProperties keycloakProperties;

    private KeycloakClient keycloakClient;

    private final String SERVER_URL = "http://localhost:8080";
    private final String REALM = "test-realm";
    private final String ADMIN_CLIENT_ID = "admin-cli";
    private final String ADMIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";
    private final String CLIENT_ID = "test-client";
    private final String CLIENT_SECRET = "test-secret";

    @BeforeEach
    void setUp() {
        lenient().when(keycloakProperties.serverUrl()).thenReturn(SERVER_URL);
        lenient().when(keycloakProperties.realm()).thenReturn(REALM);

        keycloakClient = new KeycloakClient(restTemplate, keycloakProperties);
    }

    @Test
    void adminLogin_ShouldReturnToken_WhenCredentialsAreValid() {
        // Given
        String tokenUrl = SERVER_URL + "/realms/master/protocol/openid-connect/token";
        TokenResponse expectedToken = createTokenResponse("admin-token");

        when(keycloakProperties.adminClientId()).thenReturn(ADMIN_CLIENT_ID);
        when(keycloakProperties.adminUsername()).thenReturn(ADMIN_USERNAME);
        when(keycloakProperties.adminPassword()).thenReturn(ADMIN_PASSWORD);

        when(restTemplate.postForEntity(
                eq(tokenUrl),
                any(HttpEntity.class),
                eq(TokenResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedToken));

        // When
        TokenResponse result = keycloakClient.adminLogin();

        // Then
        assertThat(result).isEqualTo(expectedToken);
    }

    @Test
    void adminLogin_ShouldThrowException_WhenCredentialsAreInvalid() {
        // Given
        String tokenUrl = SERVER_URL + "/realms/master/protocol/openid-connect/token";

        when(keycloakProperties.adminClientId()).thenReturn(ADMIN_CLIENT_ID);
        when(keycloakProperties.adminUsername()).thenReturn(ADMIN_USERNAME);
        when(keycloakProperties.adminPassword()).thenReturn(ADMIN_PASSWORD);

        when(restTemplate.postForEntity(
                eq(tokenUrl),
                any(HttpEntity.class),
                eq(TokenResponse.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        // When & Then
        assertThatThrownBy(() -> keycloakClient.adminLogin())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void registerUser_ShouldReturnUserId_WhenRegistrationIsSuccessful() {
        // Given
        String adminToken = "admin-token";
        String userRegistrationUrl = SERVER_URL + "/admin/realms/" + REALM + "/users/";
        KeycloakUserRepresentation user = new KeycloakUserRepresentation(
                "testuser", "test@example.com", true, false
        );

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(URI.create(userRegistrationUrl + "user-id-123"));

        doReturn(ResponseEntity.status(HttpStatus.CREATED)
                .headers(responseHeaders)
                .build())
                .when(restTemplate)
                .postForEntity(
                        eq(userRegistrationUrl),
                        any(HttpEntity.class),
                        eq(Void.class)
                );

        // When
        String userId = keycloakClient.registerUser(adminToken, user);

        // Then
        assertThat(userId).isEqualTo("user-id-123");
    }


    @Test
    void registerUser_ShouldThrowException_WhenResponseIsNotCreated() {
        // Given
        String adminToken = "admin-token";
        String userRegistrationUrl = SERVER_URL + "/admin/realms/" + REALM + "/users";
        KeycloakUserRepresentation user = new KeycloakUserRepresentation(
                "testuser", "test@example.com", true, false
        );

        when(restTemplate.postForEntity(
                eq(userRegistrationUrl),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        // When & Then
        assertThatThrownBy(() -> keycloakClient.registerUser(adminToken, user))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void resetUserPassword_ShouldCallKeycloakApi() {
        // Given
        String userId = "user-id-123";
        String adminToken = "admin-token";
        KeycloakCredentialsRepresentation credentials = new KeycloakCredentialsRepresentation(
                "password", "new-password", false
        );

        // When
        keycloakClient.resetUserPassword(userId, credentials, adminToken);

        // Then
        verify(restTemplate, times(1)).put(anyString(), any(HttpEntity.class));
    }

    @Test
    void assignUserRole_ShouldAssignRole_WhenRoleExists() {
        // Given
        String userId = "user-id-123";
        String adminToken = "admin-token";
        String roleName = "user";

        Object roleRepresentation = new Object();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Object.class)
        )).thenReturn(ResponseEntity.ok(roleRepresentation));

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        // When
        keycloakClient.assignUserRole(userId, roleName, adminToken);

        // Then
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class));
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void assignUserRole_ShouldNotAssignRole_WhenRoleDoesNotExist() {
        // Given
        String userId = "user-id-123";
        String adminToken = "admin-token";
        String roleName = "non-existent-role";

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Object.class)
        )).thenReturn(ResponseEntity.notFound().build());

        // When
        keycloakClient.assignUserRole(userId, roleName, adminToken);

        // Then
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class));
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void userLogin_ShouldReturnToken_WhenCredentialsAreValid() {
        // Given
        String username = "testuser";
        String password = "password";
        TokenResponse expectedToken = createTokenResponse("user-token");

        when(keycloakProperties.clientId()).thenReturn(CLIENT_ID);
        when(keycloakProperties.clientSecret()).thenReturn(CLIENT_SECRET);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(TokenResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedToken));

        // When
        TokenResponse result = keycloakClient.userLogin(username, password);

        // Then
        assertThat(result).isEqualTo(expectedToken);
    }

    @Test
    void userLogin_ShouldThrowException_WhenCredentialsAreInvalid() {
        // Given
        String username = "testuser";
        String password = "wrong-password";

        when(keycloakProperties.clientId()).thenReturn(CLIENT_ID);
        when(keycloakProperties.clientSecret()).thenReturn(CLIENT_SECRET);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(TokenResponse.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        // When & Then
        assertThatThrownBy(() -> keycloakClient.userLogin(username, password))
                .isInstanceOf(RuntimeException.class);
    }

    private TokenResponse createTokenResponse(String accessToken) {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setExpiresIn(300);
        tokenResponse.setRefreshExpiresIn(1800);
        tokenResponse.setRefreshToken("refresh-token");
        tokenResponse.setTokenType("Bearer");
        return tokenResponse;
    }
}