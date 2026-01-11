package com.urlshortener.core.ports.input;

import com.urlshortener.core.domain.models.User;
import com.urlshortener.core.domain.valueobjects.UserId;

import java.util.Optional;

/**
 * Входной порт (Use Case) для управления пользователями
 */
public interface UserManagementUseCase {

    /**
     * Создать нового пользователя
     */
    User createUser();

    /**
     * Найти пользователя по ID
     */
    Optional<User> findUser(UserId userId);

    /**
     * Найти пользователя по сокращенному ID
     */
    Optional<User> findUserByShortId(String shortId);

    /**
     * Получить текущего пользователя
     */
    Optional<User> getCurrentUser();

    /**
     * Сменить текущего пользователя
     */
    void switchUser(UserId userId);
}