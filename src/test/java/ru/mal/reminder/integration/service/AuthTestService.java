package ru.mal.reminder.integration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import ru.mal.reminder.dto.TokenResponse;
import ru.mal.reminder.dto.UserLoginRequest;
import ru.mal.reminder.dto.registration.UserRegistrationRequest;

@Service
public class AuthTestService {

    @Autowired
    private TestRestTemplate restTemplate;

    public ResponseEntity<String> register(UserRegistrationRequest request) {
        try {
            return restTemplate.postForEntity(
                    "/api/auth/register",
                    request,
                    String.class
            );
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException httpException = (HttpClientErrorException) e;
                return ResponseEntity.status(httpException.getStatusCode()).body(httpException.getResponseBodyAsString());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    public ResponseEntity<TokenResponse> login(UserLoginRequest request) {
        try {
            return restTemplate.postForEntity(
                    "/api/auth/login",
                    request,
                    TokenResponse.class
            );
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}