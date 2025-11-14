package ru.mal.reminder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mal.reminder.model.User;
import ru.mal.reminder.repository.UserRepository;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private final String KEYCLOAK_ID = "test-keycloak-id";
    private final String EMAIL = "test@example.com";
    private final String USERNAME = "testuser";

    @Test
    void findOrCreateUser_ShouldReturnExistingUser_WhenUserExists() {
        // Given
        User existingUser = createUser();

        Mockito.when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.of(existingUser));

        // When
        User result = userService.findOrCreateUser(KEYCLOAK_ID, EMAIL, USERNAME);

        // Then
        Assertions.assertThat(result).isEqualTo(existingUser);
        Mockito.verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
    }

    @Test
    void findOrCreateUser_ShouldCreateNewUser_WhenUserDoesNotExist() {
        // Given
        Mockito.when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.empty());

        User newUser = createUser();
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(newUser);

        // When
        User result = userService.findOrCreateUser(KEYCLOAK_ID, EMAIL, USERNAME);

        // Then
        Assertions.assertThat(result).isEqualTo(newUser);
        Mockito.verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
        Mockito.verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    void findOrCreateUser_ShouldSetCorrectFields_WhenCreatingNewUser() {
        // Given
        Mockito.when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.empty());

        User savedUser = createUser();
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.findOrCreateUser(KEYCLOAK_ID, EMAIL, USERNAME);

        // Then
        Mockito.verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    void findByKeycloakId_ShouldReturnUser_WhenUserExists() {
        // Given
        User user = createUser();
        Mockito.when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByKeycloakId(KEYCLOAK_ID);

        // Then
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(user);
        Mockito.verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
    }

    @Test
    void findByKeycloakId_ShouldReturnEmpty_WhenUserDoesNotExist() {
        // Given
        Mockito.when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByKeycloakId(KEYCLOAK_ID);

        // Then
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setKeycloakId(KEYCLOAK_ID);
        user.setEmail(EMAIL);
        user.setUsername(USERNAME);
        return user;
    }
}