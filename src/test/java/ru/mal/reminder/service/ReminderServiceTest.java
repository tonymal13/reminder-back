package ru.mal.reminder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import ru.mal.reminder.dto.reminder.ReminderRequest;
import ru.mal.reminder.dto.reminder.ReminderResponse;
import ru.mal.reminder.model.Reminder;
import ru.mal.reminder.model.User;
import ru.mal.reminder.repository.ReminderRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserService userService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ReminderService reminderService;

    private final String KEYCLOAK_ID = "test-keycloak-id";
    private final Long USER_ID = 1L;
    private final Long REMINDER_ID = 1L;

    @Test
    void createReminder_ShouldCreateReminder_WhenUserExists() {
        // Given
        User user = createUser();
        ReminderRequest request = createReminderRequest();
        Reminder savedReminder = createReminder(user);

        Mockito.when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        Mockito.when(reminderRepository.save(Mockito.any(Reminder.class))).thenReturn(savedReminder);

        // When
        ReminderResponse response = reminderService.createReminder(request, KEYCLOAK_ID);

        // Then
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getId()).isEqualTo(REMINDER_ID);
        Assertions.assertThat(response.getTitle()).isEqualTo(request.getTitle());
        Assertions.assertThat(response.getDescription()).isEqualTo(request.getDescription());

        Mockito.verify(userService).findByKeycloakId(KEYCLOAK_ID);
        Mockito.verify(reminderRepository).save(Mockito.any(Reminder.class));
    }

    @Test
    void createReminder_ShouldThrowException_WhenUserNotFound() {
        // Given
        ReminderRequest request = createReminderRequest();
        String errorMessage = "User not found";

        Mockito.when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
        Mockito.when(messageSource.getMessage(Mockito.eq("user.not.found"), Mockito.eq(null), Mockito.any(Locale.class)))
                .thenReturn(errorMessage);

        // When & Then
        Assertions.assertThatThrownBy(() -> reminderService.createReminder(request, KEYCLOAK_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage);

        Mockito.verify(userService).findByKeycloakId(KEYCLOAK_ID);
        Mockito.verify(reminderRepository, Mockito.never()).save(Mockito.any(Reminder.class));
    }

    @Test
    void updateReminder_ShouldUpdateReminder_WhenReminderExistsAndUserOwnsIt() {
        // Given
        User user = createUser();
        ReminderRequest request = createReminderRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");

        Reminder existingReminder = createReminder(user);
        Reminder updatedReminder = createReminder(user);
        updatedReminder.setTitle("Updated Title");
        updatedReminder.setDescription("Updated Description");

        Mockito.when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        Mockito.when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_ID))
                .thenReturn(Optional.of(existingReminder));
        Mockito.when(reminderRepository.save(Mockito.any(Reminder.class))).thenReturn(updatedReminder);

        // When
        ReminderResponse response = reminderService.updateReminder(REMINDER_ID, request, KEYCLOAK_ID);

        // Then
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getTitle()).isEqualTo("Updated Title");
        Assertions.assertThat(response.getDescription()).isEqualTo("Updated Description");

        Mockito.verify(reminderRepository).findByIdAndUserId(REMINDER_ID, USER_ID);
        Mockito.verify(reminderRepository).save(existingReminder);
    }

    @Test
    void updateReminder_ShouldThrowException_WhenReminderNotFound() {
        // Given
        User user = createUser();
        ReminderRequest request = createReminderRequest();
        String errorMessage = "Reminder not found";

        Mockito.when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        Mockito.when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_ID)).thenReturn(Optional.empty());
        Mockito.when(messageSource.getMessage(Mockito.eq("reminder.not.found"), Mockito.eq(null), Mockito.any(Locale.class)))
                .thenReturn(errorMessage);

        // When & Then
        Assertions.assertThatThrownBy(() -> reminderService.updateReminder(REMINDER_ID, request, KEYCLOAK_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage);

        Mockito.verify(reminderRepository).findByIdAndUserId(REMINDER_ID, USER_ID);
        Mockito.verify(reminderRepository, Mockito.never()).save(Mockito.any(Reminder.class));
    }

    @Test
    void deleteReminder_ShouldDeleteReminder_WhenReminderExistsAndUserOwnsIt() {
        // Given
        User user = createUser();
        Reminder reminder = createReminder(user);

        Mockito.when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        Mockito.when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_ID))
                .thenReturn(Optional.of(reminder));

        // When
        reminderService.deleteReminder(REMINDER_ID, KEYCLOAK_ID);

        // Then
        Mockito.verify(reminderRepository).findByIdAndUserId(REMINDER_ID, USER_ID);
        Mockito.verify(reminderRepository).delete(reminder);
    }

    @Test
    void getReminderById_ShouldReturnReminder_WhenReminderExistsAndUserOwnsIt() {
        // Given
        User user = createUser();
        Reminder reminder = createReminder(user);

        Mockito.when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        Mockito.when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_ID))
                .thenReturn(Optional.of(reminder));

        // When
        ReminderResponse response = reminderService.getReminderById(REMINDER_ID, KEYCLOAK_ID);

        // Then
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getId()).isEqualTo(REMINDER_ID);
        Assertions.assertThat(response.getTitle()).isEqualTo(reminder.getTitle());

        Mockito.verify(reminderRepository).findByIdAndUserId(REMINDER_ID, USER_ID);
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setKeycloakId(KEYCLOAK_ID);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        return user;
    }

    private ReminderRequest createReminderRequest() {
        ReminderRequest request = new ReminderRequest();
        request.setTitle("Test Reminder");
        request.setDescription("Test Description");
        request.setRemindDate(LocalDateTime.now().plusHours(1));
        return request;
    }

    private Reminder createReminder(User user) {
        Reminder reminder = new Reminder();
        reminder.setId(REMINDER_ID);
        reminder.setTitle("Test Reminder");
        reminder.setDescription("Test Description");
        reminder.setRemindDate(LocalDateTime.now().plusHours(1));
        reminder.setUser(user);
        return reminder;
    }
}