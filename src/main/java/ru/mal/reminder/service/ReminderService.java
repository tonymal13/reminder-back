package ru.mal.reminder.service;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mal.reminder.dto.reminder.ReminderRequest;
import ru.mal.reminder.dto.reminder.ReminderResponse;
import ru.mal.reminder.dto.reminder.SearchRequest;
import ru.mal.reminder.model.Reminder;
import ru.mal.reminder.model.User;
import ru.mal.reminder.repository.ReminderRepository;
import ru.mal.reminder.repository.specification.ReminderSpecification;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserService userService;
    private final MessageSource messageSource;

    public ReminderService(ReminderRepository reminderRepository, UserService userService, MessageSource messageSource) {
        this.reminderRepository = reminderRepository;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @Transactional
    public ReminderResponse createReminder(ReminderRequest request, String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("user.not.found", null, Locale.getDefault())
                ));

        Reminder reminder = new Reminder(
                request.getTitle(),
                request.getDescription(),
                request.getRemindDate(),
                user
        );

        Reminder savedReminder = reminderRepository.save(reminder);
        return mapToResponse(savedReminder);
    }

    @Transactional
    public ReminderResponse updateReminder(Long id, ReminderRequest request, String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("user.not.found", null, Locale.getDefault())
                ));

        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("reminder.not.found", null, Locale.getDefault())
                ));

        reminder.setTitle(request.getTitle());
        reminder.setDescription(request.getDescription());
        reminder.setRemindDate(request.getRemindDate());

        Reminder updatedReminder = reminderRepository.save(reminder);
        return mapToResponse(updatedReminder);
    }

    @Transactional
    public void deleteReminder(Long id, String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("user.not.found", null, Locale.getDefault())
                ));

        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("reminder.not.found", null, Locale.getDefault())
                ));

        reminderRepository.delete(reminder);
    }

    @Transactional(readOnly = true)
    public ReminderResponse getReminderById(Long id, String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("user.not.found", null, Locale.getDefault())
                ));

        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("reminder.not.found", null, Locale.getDefault())
                ));

        return mapToResponse(reminder);
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> searchReminders(SearchRequest searchRequest, String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("user.not.found", null, Locale.getDefault())
                ));

        Specification<Reminder> spec = ReminderSpecification.buildSearchSpecification(searchRequest, user.getId());
        Pageable pageable = createPageable(searchRequest);

        Page<Reminder> reminders = reminderRepository.findAll(spec, pageable);
        return reminders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long countReminders(SearchRequest searchRequest, String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("user.not.found", null, Locale.getDefault())
                ));

        Specification<Reminder> spec = ReminderSpecification.buildSearchSpecification(searchRequest, user.getId());
        return reminderRepository.count(spec);
    }

    private Pageable createPageable(SearchRequest searchRequest) {
        Sort sort = createSort(searchRequest);
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }

    private Sort createSort(SearchRequest searchRequest) {
        if (searchRequest.getSortBy() != null) {
            String sortField = getSortField(searchRequest.getSortBy());
            Sort.Direction direction = "desc".equalsIgnoreCase(searchRequest.getSortDirection())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            return Sort.by(direction, sortField);
        } else {
            return Sort.by(Sort.Direction.ASC, "remindDate");
        }
    }

    private String getSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "title" -> "title";
            default -> "remindDate";
        };
    }

    private ReminderResponse mapToResponse(Reminder reminder) {
        ReminderResponse response = new ReminderResponse();
        response.setId(reminder.getId());
        response.setTitle(reminder.getTitle());
        response.setDescription(reminder.getDescription());
        response.setRemindDate(reminder.getRemindDate());
        return response;
    }
}