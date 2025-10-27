package ru.mal.reminder.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import ru.mal.reminder.dto.reminder.SearchRequest;
import ru.mal.reminder.model.Reminder;

import java.time.LocalDateTime;

public class ReminderSpecification {

    public static Specification<Reminder> withUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Reminder> withTitleLike(String title) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Reminder> withDescriptionLike(String description) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
    }

    public static Specification<Reminder> withDateFrom(LocalDateTime dateFrom) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("remindDate"), dateFrom);
    }

    public static Specification<Reminder> withDateTo(LocalDateTime dateTo) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("remindDate"), dateTo);
    }

    public static Specification<Reminder> buildSearchSpecification(SearchRequest searchRequest, Long userId) {
        Specification<Reminder> spec = withUserId(userId);

        if (searchRequest.getTitle() != null && !searchRequest.getTitle().isEmpty()) {
            spec = spec.and(withTitleLike(searchRequest.getTitle()));
        }

        if (searchRequest.getDescription() != null && !searchRequest.getDescription().isEmpty()) {
            spec = spec.and(withDescriptionLike(searchRequest.getDescription()));
        }

        if (searchRequest.getDateFrom() != null) {
            spec = spec.and(withDateFrom(searchRequest.getDateFrom()));
        }

        if (searchRequest.getDateTo() != null) {
            spec = spec.and(withDateTo(searchRequest.getDateTo()));
        }

        return spec;
    }
}