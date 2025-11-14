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

import org.assertj.core.api.Assertions;
import org.mockito.Mockito;

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

        Mockito.when(reminderRepository.findByRemindDateBeforeAndNotifiedFalse(Mockito.any(LocalDateTime.class)))
                .thenReturn(dueReminders);

        Mockito.when(messageSource.getMessage(
                        Mockito.eq("reminder.notification"),
                        Mockito.any(Object[].class),
                        Mockito.any(Locale.class)
                )).thenReturn("🔔 Напоминание: Reminder 1\n📝 Description for Reminder 1\n⏰ Время: " +
                        reminder1.getRemindDate())
                .thenReturn("🔔 Напоминание: Reminder 2\n📝 Description for Reminder 2\n⏰ Время: " +
                        reminder2.getRemindDate());

        // When
        notificationService.checkDueReminders();

        // Then
        Mockito.verify(telegramService, Mockito.times(2)).sendMessage(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(reminderRepository, Mockito.times(2)).save(Mockito.any(Reminder.class));
        Assertions.assertThat(reminder1.getNotified()).isTrue();
        Assertions.assertThat(reminder2.getNotified()).isTrue();
    }

    @Test
    void checkDueReminders_ShouldNotSendNotifications_WhenNoDueReminders() {
        // Given
        Mockito.when(reminderRepository.findByRemindDateBeforeAndNotifiedFalse(Mockito.any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        notificationService.checkDueReminders();

        // Then
        Mockito.verify(telegramService, Mockito.never()).sendMessage(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(reminderRepository, Mockito.never()).save(Mockito.any(Reminder.class));
        Mockito.verify(messageSource, Mockito.never()).getMessage(Mockito.anyString(), Mockito.any(), Mockito.any());
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

        Mockito.when(messageSource.getMessage(
                Mockito.eq("reminder.notification"),
                Mockito.any(Object[].class),
                Mockito.any(Locale.class)
        )).thenReturn(expectedMessage);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // When
        notificationService.sendTelegramNotification(reminder);

        // Then
        Mockito.verify(telegramService).sendMessage(Mockito.eq("12345"), messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        Assertions.assertThat(sentMessage).isEqualTo(expectedMessage);
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