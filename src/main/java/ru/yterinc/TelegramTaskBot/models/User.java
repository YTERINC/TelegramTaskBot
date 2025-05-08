package ru.yterinc.TelegramTaskBot.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "time_zone", columnDefinition = "varchar(32) default 'UTC'")
    private String timeZone = "UTC";
}
