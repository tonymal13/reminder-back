package ru.mal.reminder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramClient telegramClient;

    public void sendMessage(String chatId, String text) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();

            telegramClient.execute(message);

        } catch (TelegramApiException e) {
            log.error("Ошибка отправки Telegram сообщения: {}", e.getMessage());
        }
    }
}