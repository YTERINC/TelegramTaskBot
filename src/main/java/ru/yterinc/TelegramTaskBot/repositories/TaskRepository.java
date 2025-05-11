package ru.yterinc.TelegramTaskBot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yterinc.TelegramTaskBot.models.Task;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Modifying
    @Query("DELETE FROM Task t WHERE t.id = :taskId AND t.userId = :userId")
    int deleteByIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);

    Task findByIdAndUserId(Long id, Long userId);

    List<Task> findByUserId(Long userId);

    List<Task> findByUserIdAndStatusIsFalse(Long userId);

}
