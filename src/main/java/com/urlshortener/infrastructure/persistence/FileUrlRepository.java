package com.urlshortener.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.urlshortener.core.domain.models.Config;
import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.ports.output.UrlRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Реализация репозитория ссылок с хранением в JSON файле
 */
public class FileUrlRepository implements UrlRepository {

    private final Map<String, ShortenedUrl> storage; // shortCode -> ShortenedUrl
    private final Map<UUID, Set<String>> userUrls;   // userId -> set of shortCodes
    private final ObjectMapper objectMapper;
    private final String storageFile;

    public FileUrlRepository(ObjectMapper objectMapper, Config config) {
        this.objectMapper = objectMapper;
        this.storageFile = config.getStorageFile();

        this.storage = new ConcurrentHashMap<>();
        this.userUrls = new ConcurrentHashMap<>();

        loadData();
    }

    @Override
    public ShortenedUrl save(ShortenedUrl url) {
        String shortCode = url.getShortCode().value();
        UUID userId = url.getUserId().value();

        // Сохраняем в памяти
        storage.put(shortCode, url);
        userUrls.computeIfAbsent(userId, k -> new HashSet<>())
                .add(shortCode);

        // Сохраняем на диск
        saveToFile();

        return url;
    }

    @Override
    public Optional<ShortenedUrl> findByShortCode(ShortCode shortCode) {
        return Optional.ofNullable(storage.get(shortCode.value()));
    }

    @Override
    public List<ShortenedUrl> findByUserId(UserId userId) {
        Set<String> userCodes = userUrls.getOrDefault(userId.value(), new HashSet<>());

        return userCodes.stream()
                .map(storage::get)
                .filter(Objects::nonNull)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ShortCode shortCode) {
        ShortenedUrl url = storage.get(shortCode.value());
        if (url == null) {
            return;
        }

        // Удаляем из всех хранилищ
        storage.remove(shortCode.value());

        UUID userId = url.getUserId().value();
        Set<String> userCodes = userUrls.get(userId);
        if (userCodes != null) {
            userCodes.remove(shortCode.value());
            if (userCodes.isEmpty()) {
                userUrls.remove(userId);
            }
        }

        saveToFile();
    }

    @Override
    public boolean existsByShortCode(ShortCode shortCode) {
        return storage.containsKey(shortCode.value());
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public long countActive() {
        return storage.values().stream()
                .filter(ShortenedUrl::canBeAccessed)
                .count();
    }

    @Override
    public long countExpired() {
        return storage.values().stream()
                .filter(url -> url.isExpired())
                .count();
    }

    @Override
    public List<ShortenedUrl> findAll() {
        return new ArrayList<>(storage.values());
    }

    private void loadData() {
        File file = new File(storageFile);

        if (!file.exists()) {
            System.out.println("ℹ️ Файл данных не найден, создаю новый");
            file.getParentFile().mkdirs();
            return;
        }

        try {
            String jsonContent = new String(Files.readAllBytes(file.toPath()));
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            JsonNode urlsNode = rootNode.get("urls");

            if (urlsNode != null && urlsNode.isArray()) {
                for (JsonNode urlNode : urlsNode) {
                    try {
                        // Читаем простые поля
                        String originalUrl = urlNode.get("originalUrl").asText();
                        String shortCode = urlNode.get("shortCode").asText();
                        String userIdStr = urlNode.get("userId").asText();
                        String createdAtStr = urlNode.get("createdAt").asText();
                        String expiresAtStr = urlNode.get("expiresAt").asText();

                        // Создаем domain объекты
                        Url urlObj = new Url(originalUrl);
                        ShortCode codeObj = new ShortCode(shortCode);
                        UserId userIdObj = UserId.fromString(userIdStr);

                        LocalDateTime createdAt = LocalDateTime.parse(createdAtStr);
                        LocalDateTime expiresAt = LocalDateTime.parse(expiresAtStr);

                        int maxClicks = urlNode.get("maxClicks").asInt();
                        int currentClicks = urlNode.get("currentClicks").asInt();
                        boolean active = urlNode.get("active").asBoolean();

                        // Создаем ShortenedUrl
                        ShortenedUrl url = new ShortenedUrl(
                                urlObj,
                                codeObj,
                                userIdObj,
                                createdAt,
                                expiresAt,
                                maxClicks,
                                currentClicks,
                                active
                        );

                        // Сохраняем в хранилищах
                        storage.put(shortCode, url);
                        userUrls.computeIfAbsent(userIdObj.value(), k -> new HashSet<>())
                                .add(shortCode);

                        // СОХРАНЯЕМ ПОЛЬЗОВАТЕЛЯ В UserRepository
                        // Это нужно делать через Application или другой механизм
                        // Но пока просто создаем пользователя если его нет

                    } catch (Exception e) {
                        System.err.println("⚠️ Ошибка загрузки записи: " + e.getMessage());
                    }
                }
            }

            System.out.println("✅ Загружено " + storage.size() + " ссылок из " + storageFile);

        } catch (IOException e) {
            System.err.println("❌ Ошибка загрузки данных: " + e.getMessage());
        }
    }

    private synchronized void saveToFile() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("urls", new ArrayList<>(storage.values()));
            data.put("metadata", Map.of(
                    "totalUrls", storage.size(),
                    "savedAt", LocalDateTime.now().toString(),
                    "version", "2.0"
            ));

            File file = new File(storageFile);
            file.getParentFile().mkdirs();

            objectMapper.writeValue(file, data);
            System.out.println("💾 Данные сохранены (" + storage.size() + " ссылок)");

        } catch (IOException e) {
            System.err.println("❌ Ошибка сохранения данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
}