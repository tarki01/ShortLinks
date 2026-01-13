package com.urlshortener.core.services;

import com.urlshortener.core.domain.models.User;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.ports.input.UserManagementUseCase;
import com.urlshortener.core.ports.output.UserRepository;

import java.util.Optional;

/**
 * Реализация для управления пользователями
 */
public class UserServiceImpl implements UserManagementUseCase {

    private final UserRepository userRepository;
    private User currentUser;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.currentUser = null;
    }

    @Override
    public User createUser() {
        UserId userId = UserId.generate();
        User user = new User(userId);

        userRepository.save(user);
        currentUser = user;
        System.out.println("Был сгенерирован идентификатор вашело пользователя");
        System.out.println("Короткий: " + userId.shortId());
        System.out.println("Полный: " + userId.value());
        return user;
    }

    @Override
    public Optional<User> findUser(UserId userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> findUserByShortId(String shortId) {
        return userRepository.findByShortId(shortId);
    }

    @Override
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    @Override
    public void switchUser(UserId userId) {
        User user = userRepository.findById(userId)
                .or(() -> {
                    // Если пользователь не найден, создаем нового
                    User newUser = new User(userId);
                    return Optional.of(userRepository.save(newUser));
                })
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        currentUser = user;
    }

    // Новый метод для переключения по строке (UUID или short ID)
    public void switchUser(String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("ID пользователя не может быть пустым");
        }

        // Пробуем как полный UUID
        try {
            UserId userId = UserId.fromString(userIdStr);
            switchUser(userId);
            return;
        } catch (IllegalArgumentException e) {
            // Не UUID, пробуем как short ID
        }

        // Ищем по короткому ID
        User user = userRepository.findByShortId(userIdStr)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Пользователь с ID " + userIdStr + " не найден"));

        currentUser = user;
    }
}