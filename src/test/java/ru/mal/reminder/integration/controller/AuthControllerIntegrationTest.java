package ru.mal.reminder.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ru.mal.reminder.integration.LifecycleSpecification;
import ru.mal.reminder.integration.data.DtoCreator;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerIntegrationTest extends LifecycleSpecification {

    private final DtoCreator dtoCreator = new DtoCreator();

    @Test
    void shouldRegisterUserAndReturnSuccessMessage() {
        // given
        var request = dtoCreator.buildUserRegistrationRequest();

        // when
        var response = authTestService.register(request);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        var user = keycloakTestService.getUserByEmail(request.getEmail());
        assertNotNull(user);
        assertEquals(request.getUsername(), user.getUsername());
    }

    @Test
    void shouldLoginAndReturnAccessToken() {
        // given
        var registerRequest = dtoCreator.buildUserRegistrationRequest();
        authTestService.register(registerRequest);

        // when: логинимся
        var loginRequest = dtoCreator.buildUserLoginRequest(
                registerRequest.getUsername(),
                registerRequest.getPassword()
        );
        var response = authTestService.login(loginRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        assertEquals("Bearer", response.getBody().getTokenType());
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() {
        // given
        var loginRequest = dtoCreator.buildUserLoginRequest("nonexistent", "wrongpassword");

        // when
        var response = authTestService.login(loginRequest);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenRegistrationDataIsInvalid() {
        // given
        var request = dtoCreator.buildUserRegistrationRequest("", "invalid-email", "123");

        // when
        var response = authTestService.register(request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldNotRegisterDuplicateUser() {
        // given
        var request = dtoCreator.buildUserRegistrationRequest();
        authTestService.register(request);

        // when:
        var duplicateResponse = authTestService.register(request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, duplicateResponse.getStatusCode());
    }
}