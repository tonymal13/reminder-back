package ru.mal.reminder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mal.reminder.model.User;
import ru.mal.reminder.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.of(existingUser));

        // When
        User result = userService.findOrCreateUser(KEYCLOAK_ID, EMAIL, USERNAME);

        // Then
        assertThat(result).isEqualTo(existingUser);
        verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
    }

    @Test
    void findOrCreateUser_ShouldCreateNewUser_WhenUserDoesNotExist() {
        // Given
        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.empty());

        User newUser = createUser();
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User result = userService.findOrCreateUser(KEYCLOAK_ID, EMAIL, USERNAME);

        // Then
        assertThat(result).isEqualTo(newUser);
        verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findOrCreateUser_ShouldSetCorrectFields_WhenCreatingNewUser() {
        // Given
        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.empty());

        User savedUser = createUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.findOrCreateUser(KEYCLOAK_ID, EMAIL, USERNAME);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findByKeycloakId_ShouldReturnUser_WhenUserExists() {
        // Given
        User user = createUser();
        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByKeycloakId(KEYCLOAK_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
    }

    @Test
    void findByKeycloakId_ShouldReturnEmpty_WhenUserDoesNotExist() {
        // Given
        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByKeycloakId(KEYCLOAK_ID);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
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