package ru.yterinc.TelegramTaskBot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yterinc.TelegramTaskBot.models.Task;
import ru.yterinc.TelegramTaskBot.repositories.TaskRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> findAll(Long chatId) {
        return taskRepository.findAll();
    }

    public Task findById(long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Transactional
    public Task addTask(Task task) {
        return taskRepository.save(task);
    }
    public boolean deleteTaskByIdAndUserId(Long taskId, Long userId) {
        Optional<Task> task = taskRepository.findById(taskId);
        if (task.isPresent() && task.get().getUserId().equals(userId)) {
            taskRepository.deleteById(taskId);
            return true;
        }
        return false;
    }




}
