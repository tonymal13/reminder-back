package ru.mal.reminder.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mal.reminder.dto.reminder.ReminderRequest;
import ru.mal.reminder.dto.reminder.ReminderResponse;
import ru.mal.reminder.dto.reminder.SearchRequest;
import ru.mal.reminder.service.ReminderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public ResponseEntity<?> createReminder(
            @Valid @RequestBody ReminderRequest request,
            @RequestHeader("X-Keycloak-Id") String keycloakId) {
        try {
            ReminderResponse response = reminderService.createReminder(request, keycloakId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReminder(
            @PathVariable Long id,
            @Valid @RequestBody ReminderRequest request,
            @RequestHeader("X-Keycloak-Id") String keycloakId) {
        try {
            ReminderResponse response = reminderService.updateReminder(id, request, keycloakId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReminder(
            @PathVariable Long id,
            @RequestHeader("X-Keycloak-Id") String keycloakId) {
        try {
            reminderService.deleteReminder(id, keycloakId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReminder(
            @PathVariable Long id,
            @RequestHeader("X-Keycloak-Id") String keycloakId) {
        try {
            ReminderResponse response = reminderService.getReminderById(id, keycloakId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchReminders(
            @RequestBody SearchRequest searchRequest,
            @RequestHeader("X-Keycloak-Id") String keycloakId) {
        try {
            List<ReminderResponse> reminders = reminderService.searchReminders(searchRequest, keycloakId);
            Long totalCount = reminderService.countReminders(searchRequest, keycloakId);

            Map<String, Object> response = new HashMap<>();
            response.put("content", reminders);
            response.put("totalElements", totalCount);
            response.put("page", searchRequest.getPage());
            response.put("size", searchRequest.getSize());
            response.put("totalPages", (int) Math.ceil((double) totalCount / searchRequest.getSize()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}