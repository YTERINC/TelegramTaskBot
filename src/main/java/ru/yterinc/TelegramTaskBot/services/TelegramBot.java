package ru.yterinc.TelegramTaskBot.services;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.yterinc.TelegramTaskBot.config.BotConfig;
import ru.yterinc.TelegramTaskBot.models.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final TaskService taskService;
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, Task> tempTasks = new ConcurrentHashMap<>();

    final BotConfig config;

    public TelegramBot(TaskService taskService, BotConfig config) {
        super(config.getToken());
        this.taskService = taskService;
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String messageText = message.getText();
            try {
                // Сначала проверяем состояние пользователя
                if (userStates.containsKey(chatId)) {
                    handleUserState(chatId, messageText);
                    return;
                }

                // Обработка основных команд
                handleCommand(chatId, messageText);
            } catch (Exception e) {
                sendMessage(chatId, "Ошибка: " + e.getMessage());
            }

        }
    }

    private void handleCommand(Long chatId, String text) {
        if (text.startsWith("/delete")) {
            handleDeleteCommand(chatId, text);
            return;
        }
        switch (text) {
            case "/start":
                sendStartMessage(chatId);
                break;
            case "/add":
                startAddTaskProcess(chatId);
                break;
            case "/list":
                listTasks(chatId);
                break;
            default:
                sendMessage(chatId, "Неизвестная команда");
        }
    }

    private void handleDeleteCommand(Long chatId, String commandText) {
        try {
            String[] parts = commandText.split(" ");
            if (parts.length < 2) {
                sendMessage(chatId, "❌ Не указан ID задачи. Формат: /delete [ID]");
                return;
            }

            long taskId = Long.parseLong(parts[1]);
            boolean isDeleted = taskService.deleteTaskByIdAndUserId(taskId, chatId);

            if (isDeleted) {
                sendMessage(chatId, "✅ Задача с ID " + taskId + " успешно удалена");
            } else {
                sendMessage(chatId, "❌ Задача не найдена или нет прав для удаления");
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Неверный формат ID. Используйте числовой идентификатор");
        } catch (Exception e) {
            sendMessage(chatId, "🚫 Ошибка при удалении задачи: " + e.getMessage());
        }
    }


    private void handleUserState(Long chatId, String text) {
        String state = userStates.get(chatId);

        if (text.startsWith("/")) {
            cancelOperation(chatId);
//            handleCommand(chatId, text);
            sendMessage(chatId, "Команда отменена");
            return;
        }

        switch (state) {
            case "AWAITING_TITLE":
                handleTitleInput(chatId, text);
                break;
            case "AWAITING_DESCRIPTION":
                handleDescriptionInput(chatId, text);
                break;
        }
    }

    private void sendStartMessage(Long chatId) {
        String welcomeText = "Добро пожаловать! Используйте команды:\n" +
                "/add - добавить задачу\n" +
                "/list - список задач\n" +
                "/delete - удалить задачу";
        sendMessage(chatId, welcomeText);
    }

    private void startAddTaskProcess(Long chatId) {
        userStates.put(chatId, "AWAITING_TITLE");
        tempTasks.put(chatId, new Task());
        sendMessage(chatId, "Введите название задачи:");
    }

    private void handleTitleInput(Long chatId, String title) {
        Task task = tempTasks.get(chatId);
        task.setTitle(title);
        userStates.put(chatId, "AWAITING_DESCRIPTION");
        sendMessage(chatId, "Теперь введите описание задачи:");
    }

    private void handleDescriptionInput(Long chatId, String description) {
        Task task = tempTasks.get(chatId);
        task.setDescription(description);
        task.setUserId(chatId);
        task.setCompleted(false);

        taskService.addTask(task);

        sendMessage(chatId, "Задача успешно создана!");
        // Очищаем временные данные
        cancelOperation(chatId);
    }

    private void cancelOperation(Long chatId) {
        userStates.remove(chatId);
        tempTasks.remove(chatId);
    }

    private void listTasks(Long chatId) {
        try {
            // Получаем задачи только текущего пользователя
            List<Task> userTasks = taskService.findAll(chatId);

            if (userTasks.isEmpty()) {
                sendMessage(chatId, "Список задач пуст.");
                return;
            }

            // Формируем красивое текстовое представление
            StringBuilder response = new StringBuilder("📝 Ваши задачи:\n\n");
            for (Task task : userTasks) {
                response.append(String.format(
                        "🆔 ID: %d\n📌 Название: %s\n📄 Описание: %s\n✅ Статус: %s\n\n",
                        task.getId(),
                        task.getTitle(),
                        task.getDescription(),
                        task.isCompleted() ? "Завершено" : "Активно"
                ));
            }

            // Добавляем подсказку по управлению
            response.append("Используйте /delete [ID] для удаления");

            sendMessage(chatId, response.toString());

        } catch (Exception e) {
            sendMessage(chatId, "❌ Ошибка при получении задач: " + e.getMessage());
            // Логирование ошибки
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


}
