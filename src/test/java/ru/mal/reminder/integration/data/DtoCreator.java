package ru.mal.reminder.integration.data;

import ru.mal.reminder.dto.UserLoginRequest;
import ru.mal.reminder.dto.registration.UserRegistrationRequest;

import java.util.UUID;

public class DtoCreator {

    public UserRegistrationRequest buildUserRegistrationRequest() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("testuser_" + uniqueId);
        request.setEmail("testuser_" + uniqueId + "@example.com");
        request.setPassword("password123");

        return request;
    }

    public UserLoginRequest buildUserLoginRequest(String username, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    public UserRegistrationRequest buildUserRegistrationRequest(String username, String email, String password) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}