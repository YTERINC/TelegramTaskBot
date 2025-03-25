package ru.yterinc.TelegramTaskBot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yterinc.TelegramTaskBot.models.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

}
