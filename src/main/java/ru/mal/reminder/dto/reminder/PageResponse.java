package ru.mal.reminder.dto.reminder;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        Long totalElements,
        int page,
        int size,
        int totalPages
) {
}
