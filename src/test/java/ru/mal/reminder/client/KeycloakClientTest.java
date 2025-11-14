package ru.mal.reminder.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.mal.reminder.config.KeycloakProperties;
import ru.mal.reminder.dto.TokenResponse;
import ru.mal.reminder.dto.keycloak.KeycloakCredentialsRepresentation;
import ru.mal.reminder.dto.keycloak.KeycloakUserRepresentation;

import java.net.URI;

import org.mockito.ArgumentMatchers;
import org.assertj.core.api.Assertions;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class KeycloakClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KeycloakProperties keycloakProperties;

    private KeycloakClient keycloakClient;

    private final String SERVER_URL = "http://localhost:8080";
    private final String REALM = "test-realm";
    private final String CLIENT_ID = "test-client";
    private final String CLIENT_SECRET = "test-secret";

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(keycloakProperties.serverUrl()).thenReturn(SERVER_URL);
        Mockito.lenient().when(keycloakProperties.realm()).thenReturn(REALM);

        keycloakClient = new KeycloakClient(restTemplate, keycloakProperties);
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

        Mockito.doReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .headers(responseHeaders)
                        .build())
                .when(restTemplate)
                .postForEntity(
                        ArgumentMatchers.eq(userRegistrationUrl),
                        ArgumentMatchers.any(HttpEntity.class),
                        ArgumentMatchers.eq(Void.class)
                );

        // When
        String userId = keycloakClient.registerUser(adminToken, user);

        // Then
        Assertions.assertThat(userId).isEqualTo("user-id-123");
    }

    @Test
    void registerUser_ShouldThrowException_WhenResponseIsNotCreated() {
        // Given
        String adminToken = "admin-token";
        String userRegistrationUrl = SERVER_URL + "/admin/realms/" + REALM + "/users";
        KeycloakUserRepresentation user = new KeycloakUserRepresentation(
                "testuser", "test@example.com", true, false
        );

        Mockito.when(restTemplate.postForEntity(
                ArgumentMatchers.eq(userRegistrationUrl),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(Void.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        // When & Then
        Assertions.assertThatThrownBy(() -> keycloakClient.registerUser(adminToken, user))
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
        Mockito.verify(restTemplate, Mockito.times(1)).put(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity.class));
    }

    @Test
    void assignUserRole_ShouldAssignRole_WhenRoleExists() {
        // Given
        String userId = "user-id-123";
        String adminToken = "admin-token";
        String roleName = "user";

        Object roleRepresentation = new Object();

        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(Object.class)
        )).thenReturn(ResponseEntity.ok(roleRepresentation));

        Mockito.when(restTemplate.postForEntity(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        // When
        keycloakClient.assignUserRole(userId, roleName, adminToken);

        // Then
        Mockito.verify(restTemplate, Mockito.times(1)).exchange(ArgumentMatchers.anyString(), ArgumentMatchers.eq(HttpMethod.GET), ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(Object.class));
        Mockito.verify(restTemplate, Mockito.times(1)).postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(Void.class));
    }

    @Test
    void assignUserRole_ShouldNotAssignRole_WhenRoleDoesNotExist() {
        // Given
        String userId = "user-id-123";
        String adminToken = "admin-token";
        String roleName = "non-existent-role";

        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(Object.class)
        )).thenReturn(ResponseEntity.notFound().build());

        // When
        keycloakClient.assignUserRole(userId, roleName, adminToken);

        // Then
        Mockito.verify(restTemplate, Mockito.times(1)).exchange(ArgumentMatchers.anyString(), ArgumentMatchers.eq(HttpMethod.GET), ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(Object.class));
        Mockito.verify(restTemplate, Mockito.never()).postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(Void.class));
    }

    @Test
    void userLogin_ShouldReturnToken_WhenCredentialsAreValid() {
        // Given
        String username = "testuser";
        String password = "password";
        TokenResponse expectedToken = createTokenResponse("user-token");

        Mockito.when(keycloakProperties.clientId()).thenReturn(CLIENT_ID);
        Mockito.when(keycloakProperties.clientSecret()).thenReturn(CLIENT_SECRET);

        Mockito.when(restTemplate.postForEntity(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(TokenResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedToken));

        // When
        TokenResponse result = keycloakClient.userLogin(username, password);

        // Then
        Assertions.assertThat(result).isEqualTo(expectedToken);
    }

    @Test
    void userLogin_ShouldThrowException_WhenCredentialsAreInvalid() {
        // Given
        String username = "testuser";
        String password = "wrong-password";

        Mockito.when(keycloakProperties.clientId()).thenReturn(CLIENT_ID);
        Mockito.when(keycloakProperties.clientSecret()).thenReturn(CLIENT_SECRET);

        Mockito.when(restTemplate.postForEntity(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(TokenResponse.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        // When & Then
        Assertions.assertThatThrownBy(() -> keycloakClient.userLogin(username, password))
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