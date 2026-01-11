package com.urlshortener.core.ports.output;

import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;

/**
 * Выходной порт для генерации ID
 */
public interface IdGenerator {

    /**
     * Сгенерировать короткий код
     */
    ShortCode generate(Url originalUrl, UserId userId);

    /**
     * Сгенерировать короткий код заданной длины
     */
    ShortCode generate(Url originalUrl, UserId userId, int length);

    /**
     * Сгенерировать короткий код для пользователя с проверкой уникальности
     */
    ShortCode generateForUser(Url originalUrl, UserId userId, int length,
                              java.util.Map<String, String> existingCodes);

    /**
     * Проверить валидность кода
     */
    boolean isValid(String code);
}