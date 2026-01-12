package com.urlshortener.core.ports.input;

import com.urlshortener.core.domain.valueobjects.UserId;

import java.util.Map;

/**
 * Входной порт для статистики
 */
public interface StatisticsUseCase {

    /**
     * Получить глобальную статистику
     */
    Map<String, Object> getGlobalStatistics();

    /**
     * Получить статистику пользователя
     */
    Map<String, Object> getUserStatistics(UserId userId);

    /**
     * Получить конфигурацию
     */
    Map<String, Object> getConfigInfo();
}