package com.urlshortener.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.core.domain.models.Config;
import com.urlshortener.core.ports.output.ConfigLoader;

import java.io.File;
import java.io.IOException;

/**
 * Загрузчик конфигурации из JSON файла
 */
public class JsonConfigLoader implements ConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = "config.json";
    private final ObjectMapper objectMapper;
    private final String configFile;

    public JsonConfigLoader(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_CONFIG_FILE);
    }

    public JsonConfigLoader(ObjectMapper objectMapper, String configFile) {
        this.objectMapper = objectMapper;
        this.configFile = configFile;
    }

    @Override
    public Config load() {
        File file = new File(configFile);
        try {
            Config config = objectMapper.readValue(file, Config.class);
            return config;
        } catch (IOException e) {
            return Config.createDefault();
        }
    }

    @Override
    public void save(Config config) {
        try {
            objectMapper.writeValue(new File(configFile), config);
        } catch (IOException e) {}
    }
}