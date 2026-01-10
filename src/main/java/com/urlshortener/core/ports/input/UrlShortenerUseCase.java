package com.urlshortener.core.ports.input;

import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Входной порт (Use Case) для операций с короткими ссылками
 */
public interface UrlShortenerUseCase {

    /**
     * Создать короткую ссылку с параметрами по умолчанию
     */
    ShortenedUrl shortenUrl(Url originalUrl, UserId userId);

    /**
     * Создать короткую ссылку с TTL в часах
     */
    ShortenedUrl shortenUrl(Url originalUrl, UserId userId, int ttlHours);

    /**
     * Создать короткую ссылку с указанием количества переходов
     */
    ShortenedUrl shortenUrlWithClicks(Url originalUrl, UserId userId, int maxClicks);

    /**
     * Создать короткую ссылку с датой истечения
     */
    ShortenedUrl shortenUrlWithExpiration(Url originalUrl, UserId userId,
                                          LocalDateTime expiresAt);

    /**
     * Создать короткую ссылку с датой истечения и ограничением переходов
     */
    ShortenedUrl shortenUrlWithExpirationAndClicks(Url originalUrl, UserId userId,
                                                   LocalDateTime expiresAt, int maxClicks);

    /**
     * Перенаправить по короткому коду
     */
    Url redirect(ShortCode shortCode);

    /**
     * Получить информацию о ссылке
     */
    ShortenedUrl getUrlInfo(ShortCode shortCode);

    /**
     * Удалить ссылку
     */
    void deleteUrl(ShortCode shortCode, UserId userId);

    /**
     * Редактировать ссылку
     */
    ShortenedUrl editUrl(ShortCode shortCode, UserId userId,
                         Url newUrl, LocalDateTime newExpiresAt);

    /**
     * Получить ссылки пользователя
     */
    List<ShortenedUrl> getUserUrls(UserId userId);

    /**
     * Проверить существование ссылки
     */
    boolean urlExists(ShortCode shortCode);

    /**
     * Проверить права пользователя на ссылку
     */
    boolean hasPermission(ShortCode shortCode, UserId userId);
}