package com.urlshortener.core.ports.output;

import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Выходной порт для хранения ссылок
 */
public interface UrlRepository {

    /**
     * Сохранить ссылку
     */
    ShortenedUrl save(ShortenedUrl url);

    /**
     * Найти ссылку по короткому коду
     */
    Optional<ShortenedUrl> findByShortCode(ShortCode shortCode);

    /**
     * Найти ссылки пользователя
     */
    List<ShortenedUrl> findByUserId(UserId userId);

    /**
     * Удалить ссылку
     */
    void delete(ShortCode shortCode);

    /**
     * Существует ли ссылка с таким кодом
     */
    boolean existsByShortCode(ShortCode shortCode);

    /**
     * Количество всех ссылок
     */
    long count();

    /**
     * Количество активных ссылок
     */
    long countActive();

    /**
     * Количество просроченных ссылок
     */
    long countExpired();

    /**
     * Все ссылки (для очистки)
     */
    List<ShortenedUrl> findAll();
}