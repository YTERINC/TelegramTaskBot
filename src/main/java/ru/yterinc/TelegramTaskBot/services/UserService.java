package ru.yterinc.TelegramTaskBot.services;

import org.springframework.stereotype.Service;
import ru.yterinc.TelegramTaskBot.models.User;
import ru.yterinc.TelegramTaskBot.repositories.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getOrCreateUser(Long chatId) {
        return userRepository.findById(chatId).orElseGet(() -> {
            User newUser = new User();
            newUser.setChatId(chatId);
            return userRepository.save(newUser);
        });
    }

    public void setUserTimeZone(Long chatId, String timeZone) {
        User user = getOrCreateUser(chatId);
        user.setTimeZone(timeZone);
        userRepository.save(user);
    }

    public String getUserTimeZone(Long chatId) {
        return getOrCreateUser(chatId).getTimeZone();
    }
}
