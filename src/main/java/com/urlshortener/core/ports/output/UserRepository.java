package com.urlshortener.core.ports.output;

import com.urlshortener.core.domain.models.User;
import com.urlshortener.core.domain.valueobjects.UserId;

import java.util.Optional;

/**
 * Выходной порт для хранения пользователей
 */
public interface UserRepository {

    /**
     * Сохранить пользователя
     */
    User save(User user);

    /**
     * Найти пользователя по ID
     */
    Optional<User> findById(UserId userId);

    /**
     * Найти пользователя по короткому ID
     */
    Optional<User> findByShortId(String shortId);

    /**
     * Количество пользователей
     */
    long count();

    /**
     * Все пользователи
     */
    Iterable<User> findAll();
}