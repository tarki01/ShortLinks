package com.urlshortener.core.domain.models;

import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Доменная модель сокращенной ссылки
 */
public class ShortenedUrl {
    @JsonProperty("originalUrl")
    private final Url originalUrl;

    @JsonProperty("shortCode")
    private final ShortCode shortCode;

    @JsonProperty("userId")
    private final UserId userId;

    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    @JsonProperty("expiresAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @JsonProperty("maxClicks")
    private int maxClicks;

    @JsonProperty("currentClicks")
    private int currentClicks;

    @JsonProperty("active")
    private boolean active;

    // Конструктор для Jackson
    public ShortenedUrl(@JsonProperty("originalUrl") Url originalUrl,
                        @JsonProperty("shortCode") ShortCode shortCode,
                        @JsonProperty("userId") UserId userId,
                        @JsonProperty("createdAt") LocalDateTime createdAt,
                        @JsonProperty("expiresAt") LocalDateTime expiresAt,
                        @JsonProperty("maxClicks") int maxClicks,
                        @JsonProperty("currentClicks") int currentClicks,
                        @JsonProperty("active") boolean active) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.userId = userId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.maxClicks = maxClicks;
        this.currentClicks = currentClicks;
        this.active = active;
    }

    public static ShortenedUrl createWithCustomExpiration(Url originalUrl, ShortCode shortCode,
                                                          UserId userId, LocalDateTime expiresAt,
                                                          int maxClicks) {
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Дата истечения должна быть в будущем!");
        }

        return new ShortenedUrl(
                originalUrl,
                shortCode,
                userId,
                LocalDateTime.now(),
                expiresAt,
                maxClicks,
                0,
                true
        );
    }

    @JsonIgnore
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    @JsonIgnore
    public boolean canBeAccessed() {
        return active && !isExpired() && currentClicks < maxClicks;
    }

    @JsonIgnore
    public String getShortUrl(String baseUrl) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + shortCode.value();
    }

    @JsonIgnore
    public long getRemainingHours() {
        return Math.max(0, ChronoUnit.HOURS.between(LocalDateTime.now(), expiresAt));
    }

    @JsonIgnore
    public int getRemainingClicks() {
        return Math.max(0, maxClicks - currentClicks);
    }

    public void incrementClicks() {
        if (!canBeAccessed()) {
            throw new IllegalStateException("Невозможно увеличить количество переходов на несуществующей ссылке!");
        }

        this.currentClicks++;
        if (this.currentClicks >= this.maxClicks) {
            this.active = false;
        }
    }

    public boolean updateUrl(Url newUrl) {
        if (newUrl == null || this.originalUrl.equals(newUrl)) {
            return false;
        }

        return false;
    }

    public boolean updateExpiration(LocalDateTime newExpiresAt, LocalDateTime maxExpiresAt) {
        if (newExpiresAt == null || newExpiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        if (maxExpiresAt != null && newExpiresAt.isAfter(maxExpiresAt)) {
            return false;
        }
        this.expiresAt = newExpiresAt;
        return true;
    }

    @JsonIgnore
    public String getStatus() {
        if (!active) {
            return "Заблокирована";
        }
        if (isExpired()) {
            return "Истекла";
        }
        if (currentClicks >= maxClicks) {
            return "Лимит исчерпан";
        }
        return "Активна";
    }

    // Геттеры (нужны для Jackson)
    public Url getOriginalUrl() {
        return originalUrl;
    }

    public ShortCode getShortCode() {
        return shortCode;
    }

    public UserId getUserId() {
        return userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getMaxClicks() {
        return maxClicks;
    }


    public int getCurrentClicks() {
        return currentClicks;
    }

    // Метод для создания копии с обновленными параметрами
    public ShortenedUrl withUpdatedParams(Url newUrl, LocalDateTime newExpiresAt) {
        return new ShortenedUrl(
                newUrl != null ? newUrl : this.originalUrl,
                this.shortCode,
                this.userId,
                this.createdAt,
                newExpiresAt != null ? newExpiresAt : this.expiresAt,
                this.maxClicks,
                this.currentClicks,
                this.active
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShortenedUrl that = (ShortenedUrl) o;
        return Objects.equals(shortCode, that.shortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortCode);
    }

    @Override
    public String toString() {
        return String.format("ShortenedUrl{shortCode='%s', originalUrl='%s', userId=%s}",
                shortCode.value(),
                originalUrl.value().length() > 30 ?
                        originalUrl.value().substring(0, 27) + "..." :
                        originalUrl.value(),
                userId.shortId());
    }
}