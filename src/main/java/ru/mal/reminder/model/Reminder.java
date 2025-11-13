package ru.mal.reminder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminder")
@Getter
@Setter
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4096)
    private String description;

    @Column(name = "remind", nullable = false)
    private LocalDateTime remindDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notified", nullable = false)
    private Boolean notified = false;

    public Reminder() {}

    public Reminder(String title, String description, LocalDateTime remindDate, User user) {
        this.title = title;
        this.description = description;
        this.remindDate = remindDate;
        this.user = user;
    }

}