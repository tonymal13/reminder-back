package ru.mal.reminder.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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

import static ru.mal.reminder.Consts.*;

@Slf4j
@Component
@RequiredArgsConstructor
@Getter
@Setter
public class KeycloakClient {

    private final RestTemplate restTemplate;
    private final KeycloakProperties properties;

    public TokenResponse adminLogin() {
        String tokenUrl = properties.serverUrl() + ADMIN_TOKEN_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE_PARAM, GRANT_TYPE_PASSWORD);
        formData.add(CLIENT_ID_PARAM, properties.adminClientId());
        formData.add(USERNAME_PARAM, properties.adminUsername());
        formData.add(PASSWORD_PARAM, properties.adminPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, request, TokenResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException();
        }
    }

    public String registerUser(String adminToken, KeycloakUserRepresentation user) {
        String userRegistrationUrl = properties.serverUrl() + ADMIN_REALMS_PATH + properties.realm() + USERS_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<KeycloakUserRepresentation> request = new HttpEntity<>(user, headers);

        ResponseEntity<Void> response = restTemplate.postForEntity(userRegistrationUrl, request, Void.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            String location = Objects.requireNonNull(response.getHeaders().getLocation()).toString();
            return location.substring(location.lastIndexOf('/') + 1);
        } else {
            throw new RuntimeException();
        }
    }

    public void resetUserPassword(String userId, KeycloakCredentialsRepresentation credentials, String adminToken) {
        String passwordResetUrl = properties.serverUrl() + ADMIN_REALMS_PATH + properties.realm() + USERS_PATH + userId + RESET_PASSWORD_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<KeycloakCredentialsRepresentation> request = new HttpEntity<>(credentials, headers);

        restTemplate.put(passwordResetUrl, request);
    }

    public void assignUserRole(String userId, String roleName, String adminToken) {
        String rolesUrl = properties.serverUrl() + ADMIN_REALMS_PATH + properties.realm() + USERS_PATH + userId + ROLE_MAPPINGS_PATH;
        String roleUrl = properties.serverUrl() + ADMIN_REALMS_PATH + properties.realm() + ROLES_PATH + roleName;

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
    }

    public TokenResponse userLogin(String username, String password) {
        String tokenUrl = properties.serverUrl() + REALMS_PATH + properties.realm() + TOKEN_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE_PARAM, GRANT_TYPE_PASSWORD);
        formData.add(CLIENT_ID_PARAM, properties.clientId());
        formData.add(CLIENT_SECRET_PARAM, properties.clientSecret());
        formData.add(USERNAME_PARAM, username);
        formData.add(PASSWORD_PARAM, password);

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(formData, headers);

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                tokenUrl, tokenRequest, TokenResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException();
        }
    }
}