package ru.mal.reminder.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.mal.reminder.model.Reminder;
import ru.mal.reminder.repository.ReminderRepository;
import org.springframework.context.MessageSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderNotificationService {

    private final ReminderRepository reminderRepository;
    private final TelegramService telegramService;
    private final MessageSource messageSource;

    @Scheduled(fixedRateString = "${scheduler.check-interval:60000}")
    @Transactional
    public void checkDueReminders() {
        List<Reminder> dueReminders = reminderRepository
                .findByRemindDateBeforeAndNotifiedFalse(LocalDateTime.now());

        for (Reminder reminder : dueReminders) {
            sendTelegramNotification(reminder);
            reminder.setNotified(true);
            reminderRepository.save(reminder);
        }
    }

    public void sendTelegramNotification(Reminder reminder) {
        String chatId = reminder.getUser().getChatId();

        String message = messageSource.getMessage(
                "reminder.notification",
                new Object[]{
                        reminder.getTitle(),
                        reminder.getDescription(),
                        reminder.getRemindDate()
                },
                Locale.getDefault()
        );

        telegramService.sendMessage(chatId, message);
    }
}