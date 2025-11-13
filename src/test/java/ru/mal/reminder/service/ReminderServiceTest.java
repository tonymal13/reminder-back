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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(reminderRepository.save(any(Reminder.class))).thenReturn(savedReminder);

        // When
        ReminderResponse response = reminderService.createReminder(request, KEYCLOAK_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(REMINDER_ID);
        assertThat(response.getTitle()).isEqualTo(request.getTitle());
        assertThat(response.getDescription()).isEqualTo(request.getDescription());

        verify(userService).findByKeycloakId(KEYCLOAK_ID);
        verify(reminderRepository).save(any(Reminder.class));
    }

    @Test
    void createReminder_ShouldThrowException_WhenUserNotFound() {
        // Given
        ReminderRequest request = createReminderRequest();
        String errorMessage = "User not found";

        when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
        when(messageSource.getMessage(eq("user.not.found"), eq(null), any(Locale.class)))
                .thenReturn(errorMessage);

        // When & Then
        assertThatThrownBy(() -> reminderService.createReminder(request, KEYCLOAK_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage);

        verify(userService).findByKeycloakId(KEYCLOAK_ID);
        verify(reminderRepository, never()).save(any(Reminder.class));
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

        when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_ID))
                .thenReturn(Optional.of(existingReminder));
        when(reminderRepository.save(any(Reminder.class))).thenReturn(updatedReminder);

        // When
        ReminderResponse response = reminderService.updateReminder(REMINDER_ID, request, KEYCLOAK_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getDescription()).isEqualTo("Updated Description");

        verify(reminderRepository).findByIdAndUserId(REMINDER_ID, USER_ID);
        verify(reminderRepository).save(existingReminder);
    }

    @Test
    void updateReminder_ShouldThrowException_WhenReminderNotFound() {
        // Given
        User user = createUser();
        ReminderRequest request = createReminderRequest();
        String errorMessage = "Reminder not found";

        when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_ID)).thenReturn(Optional.empty());
        when(messageSource.getMessage(eq("reminder.not.found"), eq(null), any(Locale.class)))
                .thenReturn(errorMessage);

        // When & Then
        assertThatThrownBy(() -> reminderService.updateReminder(REMINDER_ID, request, KEYCLOAK_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage);

        verify(reminderRepository).findByIdAndUserId(REMINDER_ID, USER_ID);
        verify(reminderRepository, never()).save(any(Reminder.class));
    }

    @Test
    void deleteReminder_ShouldDeleteReminder_WhenReminderExistsAndUserOwnsIt() {
        // Given
        User user = createUser();
        Reminder reminder = createReminder(user);

        when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_ID))
                .thenReturn(Optional.of(reminder));

        // When
        reminderService.deleteReminder(REMINDER_ID, KEYCLOAK_ID);

        // Then
        verify(reminderRepository).findByIdAndUserId(REMINDER_ID, USER_ID);
        verify(reminderRepository).delete(reminder);
    }

    @Test
    void getReminderById_ShouldReturnReminder_WhenReminderExistsAndUserOwnsIt() {
        // Given
        User user = createUser();
        Reminder reminder = createReminder(user);

        when(userService.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_ID))
                .thenReturn(Optional.of(reminder));

        // When
        ReminderResponse response = reminderService.getReminderById(REMINDER_ID, KEYCLOAK_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(REMINDER_ID);
        assertThat(response.getTitle()).isEqualTo(reminder.getTitle());

        verify(reminderRepository).findByIdAndUserId(REMINDER_ID, USER_ID);
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