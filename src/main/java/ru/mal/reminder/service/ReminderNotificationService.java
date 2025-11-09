package ru.mal.reminder.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.mal.reminder.model.Reminder;
import ru.mal.reminder.repository.ReminderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderNotificationService {

    private final ReminderRepository reminderRepository;
    private final TelegramService telegramService;

    @Scheduled(fixedRate = 60000)
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

    private void sendTelegramNotification(Reminder reminder) {
        String chatId = reminder.getUser().getChatId();

        String message = String.format(
                "🔔 Напоминание: %s\n📝 %s\n⏰ Время: %s",
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getRemindDate()
        );

        telegramService.sendMessage(chatId, message);
    }
}