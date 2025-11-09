package ru.mal.reminder.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mal.reminder.dto.reminder.PageResponse;
import ru.mal.reminder.dto.reminder.ReminderRequest;
import ru.mal.reminder.dto.reminder.ReminderResponse;
import ru.mal.reminder.dto.reminder.SearchRequest;
import ru.mal.reminder.service.ReminderService;

import java.util.List;

import static ru.mal.reminder.Consts.KEYCLOAK_HEADER;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> createReminder(
            @Valid @RequestBody ReminderRequest request,
            @RequestHeader(KEYCLOAK_HEADER) String keycloakId) {
        ReminderResponse response = reminderService.createReminder(request, keycloakId);
        return ResponseEntity.ok(response);

    }

    @PutMapping("/{id}")
    public ResponseEntity<ReminderResponse> updateReminder(
            @PathVariable Long id,
            @Valid @RequestBody ReminderRequest request,
            @RequestHeader(KEYCLOAK_HEADER) String keycloakId) {
        ReminderResponse response = reminderService.updateReminder(id, request, keycloakId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(
            @PathVariable Long id,
            @RequestHeader(KEYCLOAK_HEADER) String keycloakId) {
        reminderService.deleteReminder(id, keycloakId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReminderResponse> getReminder(
            @PathVariable Long id,
            @RequestHeader(KEYCLOAK_HEADER) String keycloakId) {
        ReminderResponse response = reminderService.getReminderById(id, keycloakId);
        return ResponseEntity.ok(response);

    }

    @PostMapping("/search")
    public ResponseEntity<PageResponse<ReminderResponse>> searchReminders(
            @RequestBody SearchRequest searchRequest,
            @RequestHeader(KEYCLOAK_HEADER) String keycloakId) {

        List<ReminderResponse> reminders = reminderService.searchReminders(searchRequest, keycloakId);
        Long totalCount = reminderService.countReminders(searchRequest, keycloakId);

        PageResponse<ReminderResponse> response = new PageResponse<>(
                reminders, totalCount, searchRequest.getPage(),
                searchRequest.getSize(), (int) Math.ceil((double) totalCount / searchRequest.getSize())
        );

        return ResponseEntity.ok(response);
    }
}