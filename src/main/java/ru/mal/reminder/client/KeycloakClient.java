package ru.mal.reminder.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.mal.reminder.config.KeycloakProperties;
import ru.mal.reminder.dto.keycloak.KeycloakCredentialsRepresentation;
import ru.mal.reminder.dto.keycloak.KeycloakUserRepresentation;
import ru.mal.reminder.dto.TokenResponse;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakClient {

    private final RestTemplate restTemplate;
    private final KeycloakProperties properties;

    public TokenResponse adminLogin() {
        String tokenUrl = properties.serverUrl() + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", properties.adminClientId());
        formData.add("username", properties.adminUsername());
        formData.add("password", properties.adminPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, request, TokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Admin login failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Admin login error: " + e.getMessage());
        }
    }

    public String registerUser(String adminToken, KeycloakUserRepresentation user) {
        String userRegistrationUrl = properties.serverUrl() + "/admin/realms/" + properties.realm() + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<KeycloakUserRepresentation> request = new HttpEntity<>(user, headers);

        ResponseEntity<Void> response = restTemplate.postForEntity(userRegistrationUrl, request, Void.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            String location = Objects.requireNonNull(response.getHeaders().getLocation()).toString();
            return location.substring(location.lastIndexOf('/') + 1);
        } else {
            throw new RuntimeException("User registration failed: " + response.getStatusCode());
        }
    }

    public void resetUserPassword(String userId, KeycloakCredentialsRepresentation credentials, String adminToken) {
        String passwordResetUrl = properties.serverUrl() + "/admin/realms/" + properties.realm() + "/users/" + userId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<KeycloakCredentialsRepresentation> request = new HttpEntity<>(credentials, headers);

        restTemplate.put(passwordResetUrl, request);
    }

    public void assignUserRole(String userId, String roleName, String adminToken) {
        try {
            String rolesUrl = properties.serverUrl() + "/admin/realms/" + properties.realm() + "/users/" + userId + "/role-mappings/realm";
            String roleUrl = properties.serverUrl() + "/admin/realms/" + properties.realm() + "/roles/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<Object> roleResponse = restTemplate.exchange(
                    roleUrl, HttpMethod.GET, getRequest, Object.class);

            if (roleResponse.getStatusCode() == HttpStatus.OK && roleResponse.getBody() != null) {
                Object roleRepresentation = roleResponse.getBody();
                List<Object> rolesList = List.of(roleRepresentation);

                HttpEntity<List<Object>> assignRequest = new HttpEntity<>(rolesList, headers);
                restTemplate.postForEntity(rolesUrl, assignRequest, Void.class);
            }
        } catch (Exception e) {
            log.error("Failed to assign role '{}' to user {}: {}", roleName, userId, e.getMessage());
        }
    }
}