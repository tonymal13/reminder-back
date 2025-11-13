package ru.mal.reminder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.mal.reminder.model.Reminder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long>, JpaSpecificationExecutor<Reminder> {

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM Reminder r WHERE r.remindDate < :now AND r.notified = false")
    List<Reminder> findByRemindDateBeforeAndNotifiedFalse(@Param("now") LocalDateTime now);
}