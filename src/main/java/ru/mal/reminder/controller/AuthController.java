package ru.mal.reminder.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ru.mal.reminder.config.KeycloakProperties;
import ru.mal.reminder.dto.TokenResponse;
import ru.mal.reminder.dto.UserLoginRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RestTemplate restTemplate;
    private final KeycloakProperties properties;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        try {
            String tokenUrl = properties.serverUrl() + "/realms/" + properties.realm() + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", properties.clientId());
            formData.add("client_secret", properties.clientSecret());
            formData.add("username", request.getUsername());
            formData.add("password", request.getPassword());

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(formData, headers);

            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    tokenUrl, tokenRequest, TokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Login failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login error: " + e.getMessage());
        }
    }
}