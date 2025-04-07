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
        return taskRepository.findAll();
    }

    public Task findById(long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Transactional
    public Task addTask(Task task) {
        return taskRepository.save(task);
    }
//    @Transactional
//    public boolean deleteTaskByIdAndUserId(Long taskId, Long userId) {
//        Optional<Task> task = taskRepository.findById(taskId);
//        if (task.isPresent() && task.get().getUserId().equals(userId)) {
//            taskRepository.deleteById(taskId);x
//            return true;
//        }
//        return false;
//    }

    @Transactional
    public boolean deleteTaskByIdAndUserId(Long taskId, Long userId) {
        return taskRepository.deleteByIdAndUserId(taskId, userId) > 0;
    }
//TODO

    @Transactional
    public boolean completeTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId);
        if (task != null && !task.isCompleted()) {
            task.setCompleted(true);
            taskRepository.save(task);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean inCompleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId);
        if (task != null && task.isCompleted()) {
            task.setCompleted(false);
            taskRepository.save(task);
            return true;
        }
        return false;
    }


}
