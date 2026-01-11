package com.urlshortener.core.ports.output;

import com.urlshortener.core.domain.models.Config;

/**
 * Выходной порт для загрузки конфигурации
 */
public interface ConfigLoader {

    /**
     * Загрузить конфигурацию
     */
    Config load();

    /**
     * Сохранить конфигурацию
     */
    void save(Config config);
}