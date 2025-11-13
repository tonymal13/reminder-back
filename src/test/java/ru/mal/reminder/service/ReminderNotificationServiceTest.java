package ru.mal.reminder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import ru.mal.reminder.model.Reminder;
import ru.mal.reminder.model.User;
import ru.mal.reminder.repository.ReminderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderNotificationServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private TelegramService telegramService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ReminderNotificationService notificationService;

    @Test
    void checkDueReminders_ShouldSendNotifications_WhenDueRemindersExist() {
        // Given
        User user = new User();
        user.setChatId("12345");

        Reminder reminder1 = createReminder(1L, "Reminder 1", user);
        Reminder reminder2 = createReminder(2L, "Reminder 2", user);

        List<Reminder> dueReminders = List.of(reminder1, reminder2);

        when(reminderRepository.findByRemindDateBeforeAndNotifiedFalse(any(LocalDateTime.class)))
                .thenReturn(dueReminders);

        when(messageSource.getMessage(
                eq("reminder.notification"),
                any(Object[].class),
                any(Locale.class)
        )).thenReturn("🔔 Напоминание: Reminder 1\n📝 Description for Reminder 1\n⏰ Время: " +
                        reminder1.getRemindDate())
                .thenReturn("🔔 Напоминание: Reminder 2\n📝 Description for Reminder 2\n⏰ Время: " +
                        reminder2.getRemindDate());

        // When
        notificationService.checkDueReminders();

        // Then
        verify(telegramService, times(2)).sendMessage(anyString(), anyString());
        verify(reminderRepository, times(2)).save(any(Reminder.class));
        assertThat(reminder1.getNotified()).isTrue();
        assertThat(reminder2.getNotified()).isTrue();
    }

    @Test
    void checkDueReminders_ShouldNotSendNotifications_WhenNoDueReminders() {
        // Given
        when(reminderRepository.findByRemindDateBeforeAndNotifiedFalse(any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        notificationService.checkDueReminders();

        // Then
        verify(telegramService, never()).sendMessage(anyString(), anyString());
        verify(reminderRepository, never()).save(any(Reminder.class));
        verify(messageSource, never()).getMessage(anyString(), any(), any());
    }

    @Test
    void sendTelegramNotification_ShouldFormatMessageCorrectly() {
        // Given
        User user = new User();
        user.setChatId("12345");

        LocalDateTime remindDate = LocalDateTime.of(2024, 1, 1, 10, 0);
        Reminder reminder = createReminder(1L, "Test Title", user);
        reminder.setDescription("Test Description");
        reminder.setRemindDate(remindDate);

        String expectedMessage = "🔔 Напоминание: Test Title\n📝 Test Description\n⏰ Время: 01.01.2024 10:00";

        when(messageSource.getMessage(
                eq("reminder.notification"),
                any(Object[].class),
                any(Locale.class)
        )).thenReturn(expectedMessage);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // When
        notificationService.sendTelegramNotification(reminder);

        // Then
        verify(telegramService).sendMessage(eq("12345"), messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isEqualTo(expectedMessage);
    }

    private Reminder createReminder(Long id, String title, User user) {
        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setTitle(title);
        reminder.setDescription("Description for " + title);
        reminder.setRemindDate(LocalDateTime.now().minusMinutes(5));
        reminder.setUser(user);
        reminder.setNotified(false);
        return reminder;
    }
}