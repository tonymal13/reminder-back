package ru.mal.reminder.integration.service;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.mal.reminder.client.KeycloakClient;
import ru.mal.reminder.dto.TokenResponse;

import java.util.List;

@Service
public class KeycloakTestService {

    private final KeycloakClient keycloakClient;

    @Autowired
    public KeycloakTestService(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    public UserRepresentation getUserByEmail(String email) {
        TokenResponse adminToken = keycloakClient.adminLogin();

        return findUserByEmail(email, adminToken.getAccessToken());
    }

    private UserRepresentation findUserByEmail(String email, String adminToken) {
        String usersUrl = keycloakClient.getProperties().serverUrl() +
                "/admin/realms/" + keycloakClient.getProperties().realm() +
                "/users?email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ParameterizedTypeReference<List<UserRepresentation>> typeRef =
                new ParameterizedTypeReference<>() {
                };

        ResponseEntity<List<UserRepresentation>> response = keycloakClient.getRestTemplate()
                .exchange(usersUrl, HttpMethod.GET, request, typeRef);

        return response.getBody() != null && !response.getBody().isEmpty()
                ? response.getBody().getFirst()
                : null;
    }

}