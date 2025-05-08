package ru.yterinc.TelegramTaskBot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yterinc.TelegramTaskBot.models.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
