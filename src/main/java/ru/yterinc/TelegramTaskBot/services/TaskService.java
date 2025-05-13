package ru.yterinc.TelegramTaskBot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yterinc.TelegramTaskBot.models.Task;
import ru.yterinc.TelegramTaskBot.repositories.TaskRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> findAll(Long chatId) {
        return taskRepository.findByChatId(chatId);
    }

    public List<Task> findActiveAll(Long chatId) {
        return taskRepository.findByChatIdAndStatusIsFalse(chatId);
    }

    public Task findById(long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Transactional
    public void addTask(Task task) {
        taskRepository.save(task);
    }

    @Transactional
    public boolean deleteTaskByIdAndUserId(Long taskId, Long userId) {
        return taskRepository.deleteByIdAndChatId(taskId, userId) > 0;
    }

    @Transactional
    public boolean completeTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndChatId(taskId, userId);
        if (task != null && !task.isStatus()) {
            task.setStatus(true);
            taskRepository.save(task);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean inCompleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndChatId(taskId, userId);
        if (task != null && task.isStatus()) {
            task.setStatus(false);
            taskRepository.save(task);
            return true;
        }
        return false;
    }

}
