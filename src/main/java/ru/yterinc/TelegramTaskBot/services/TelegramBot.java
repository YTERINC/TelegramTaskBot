package ru.yterinc.TelegramTaskBot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.yterinc.TelegramTaskBot.config.BotConfig;
import ru.yterinc.TelegramTaskBot.models.Task;
import ru.yterinc.TelegramTaskBot.models.UserStatesType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final TaskService taskService;
    private final Map<Long, UserStatesType> userStates = new HashMap<>();
    private final Map<Long, Task> tempTasks = new ConcurrentHashMap<>();
    final BotConfig config;
    private static final String EMPTY = "empty";

    public TelegramBot(TaskService taskService, BotConfig config) {
        super(config.getToken());
        this.taskService = taskService;
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Помощь"));
        listOfCommands.add(new BotCommand("/add", "Добавить задачу"));
        listOfCommands.add(new BotCommand("/list_all", "Список всех задач"));
        listOfCommands.add(new BotCommand("/list_act", "Список активных задач"));
        listOfCommands.add(new BotCommand("/cancel", "Отмена"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }
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
                logError(e, chatId);
            }
        }
    }

    private void handleUserState(Long chatId, String text) {
        UserStatesType state = userStates.get(chatId);

        if (text.startsWith("/cancel")) {
            cancelOperation(chatId);
            sendMessage(chatId, "Команда отменена");
            return;
        }

        switch (state) {
            case TITLE:
                handleTitleInput(chatId, text);
                break;
            case DESCRIPTION:
                handleDescriptionInput(chatId, text);
                break;
        }
    }

    private void handleCommand(Long chatId, String text) {
        switch (text) {
            case "/start":
                sendStartMessage(chatId);
                break;
            case "/add":
                startAddTaskProcess(chatId);
                break;
            case "/list_act":
                listActiveTasks(chatId);
                break;
            case "/list_all":
                listAllTasks(chatId);
                break;
            case "/cancel":
                sendMessage(chatId, "Команда используется для отмены создаваемой задачи");
                break;
            default:
                sendMessage(chatId, "Неизвестная команда");
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();
        // Проверка нажатия кнопки "Удалить"
        if (data.startsWith("delete_")) {
            long taskId = Long.parseLong(data.split("_")[1]);
            deleteTask(chatId, taskId, callbackQuery.getMessage().getMessageId());
        }

        // Проверка нажатия кнопки "Завершить"
        if (data.startsWith("complete_")) {
            long taskId = Long.parseLong(data.split("_")[1]);
            completeTask(chatId, taskId, callbackQuery.getMessage().getMessageId());
        }

        // Проверка нажатия кнопки "Вернуть задачу"
        if (data.startsWith("incomplete_")) {
            long taskId = Long.parseLong(data.split("_")[1]);
            cancelCompletionTask(chatId, taskId, callbackQuery.getMessage().getMessageId());
        }
    }

    private void sendStartMessage(Long chatId) {
        String welcomeText = """
                Добро пожаловать! Используйте команды:
                /add - добавить задачу
                /list_act - список активных задач
                /list_all - список всех задач
                /cancel - отмена
                """;
        sendMessage(chatId, welcomeText);
    }

    private void startAddTaskProcess(Long chatId) {
        userStates.put(chatId, UserStatesType.TITLE);
        tempTasks.put(chatId, new Task());
        sendMessage(chatId, "Введите название задачи:");
    }

    private void handleTitleInput(Long chatId, String title) {
        Task task = tempTasks.get(chatId);
        task.setTitle(title);
        userStates.put(chatId, UserStatesType.DESCRIPTION);
        sendMessage(chatId, "Введите описание или поставьте любой символ для задачи без описания:");
    }

    private void handleDescriptionInput(Long chatId, String description) {
        Task task = tempTasks.get(chatId);
        if (description.getBytes().length < 3) description = EMPTY;
        task.setDescription(description);
        task.setChatId(chatId);
        task.setStatus(false);

        taskService.addTask(task);

        sendMessage(chatId, "Задача успешно создана!");
        log.info("Task created, ID = {}", task.getId());
        // Очищаем временные данные
        cancelOperation(chatId);
    }

    private void cancelOperation(Long chatId) {
        userStates.remove(chatId);
        tempTasks.remove(chatId);
    }

    private void listAllTasks(Long chatId) {
        try {
            List<Task> userTasks = taskService.findAll(chatId);
            if (userTasks.isEmpty()) {
                sendMessage(chatId, "Список всех задач пуст.");
                return;
            }
            for (Task task : userTasks) {
                createTaskMessage(task);
            }
        } catch (Exception e) {
            logError(e, chatId);

        }
    }

    private void listActiveTasks(Long chatId) {
        try {
            List<Task> userTasks = taskService.findActiveAll(chatId);
            if (userTasks.isEmpty()) {
                sendMessage(chatId, "Список активных задач пуст");
                return;
            }
            for (Task task : userTasks) {
                createTaskMessage(task);
            }
        } catch (Exception e) {
            logError(e, chatId);
        }
    }

    private void createTaskMessage(Task task) {
        // Создаем строку с текстом задачи и кнопкой
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        rows.add(row);
        // Текст задачи с условием
        String description = task.getDescription();
        String taskText;
        if (Objects.equals(description, EMPTY)) {
            taskText = String.format("📌 %s\n%s\n\n",
                    task.getTitle(), task.isStatus() ? "✅ Завершено" : "🔄 Активно");
        } else {
            taskText = String.format("📌 %s\n📄 %s\n%s\n\n",
                    task.getTitle(), description, task.isStatus() ? "✅ Завершено" : "🔄 Активно");
        }

        // Кнопка удаления
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("❌ Удалить");
        deleteButton.setCallbackData("delete_" + task.getId());
        row.add(deleteButton);

        // Кнопки для завершения и возвращения задачи
        InlineKeyboardButton completeButton = new InlineKeyboardButton();
        if (!task.isStatus()) {
            completeButton.setText("✅ Завершить");
            completeButton.setCallbackData("complete_" + task.getId());
        } else {
            completeButton.setText("⬆️ Вернуть задачу");
            completeButton.setCallbackData("incomplete_" + task.getId());
        }

        row.add(completeButton);
        // Отправляем каждую задачу отдельным сообщением с кнопкой
        SendMessage message = new SendMessage();
        message.setChatId(task.getChatId().toString());
        message.setText(taskText);

        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logError(e, task.getChatId());
        }
    }

    private void deleteTask(Long chatId, Long taskId, Integer messageId) {
        try {
            boolean isDeleted = taskService.deleteTaskByIdAndUserId(taskId, chatId);

            if (isDeleted) {
                // Удаляем оригинальное сообщение с задачей
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId.toString());
                deleteMessage.setMessageId(messageId);
                execute(deleteMessage);

                // Отправляем подтверждение
                sendMessage(chatId, "Задача успешно удалена ✅");
                log.info("Task deleted, ID = {}", taskId);
            } else {
                sendMessage(chatId, "❌ Ошибка удаления: задача не найдена");
            }
        } catch (Exception e) {
            logError(e, chatId);
        }
    }

    private void completeTask(Long chatId, Long taskId, Integer messageId) {
        try {
            boolean isCompleted = taskService.completeTask(taskId, chatId);

            if (isCompleted) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId.toString());
                deleteMessage.setMessageId(messageId);
                execute(deleteMessage);

                // Отправляем подтверждение
                sendMessage(chatId, "Задача завершена ✅"); // можно и убрать
                createTaskMessage(taskService.findById(taskId));
            } else {
                sendMessage(chatId, "❌ Ошибка завершения задачи");
            }
        } catch (Exception e) {
            logError(e, chatId);
        }
    }

    private void cancelCompletionTask(Long chatId, Long taskId, Integer messageId) {
        try {
            boolean inCompleted = taskService.inCompleteTask(taskId, chatId);

            if (inCompleted) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId.toString());
                deleteMessage.setMessageId(messageId);
                execute(deleteMessage);

                // Отправляем подтверждение
                sendMessage(chatId, "Задача возвращена в работу ✅"); // можно и убрать
                createTaskMessage(taskService.findById(taskId));
            } else {
                sendMessage(chatId, "❌ Ошибка возвращения задачи");
            }
        } catch (Exception e) {
            logError(e, chatId);
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logError(e, chatId);
        }
    }

    private void logError(Exception e, Long chatId) {
        log.error("Error (chatId = {}): ", chatId, e);
    }

}
