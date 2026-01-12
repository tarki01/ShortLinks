package com.urlshortener.core.domain.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Конфигурация приложения
 */
public class Config {
    @JsonProperty("baseUrl")
    private String baseUrl;

    @JsonProperty("defaultTTLHours")
    private int defaultTTLHours;

    @JsonProperty("defaultMaxClicks")
    private int defaultMaxClicks;

    @JsonProperty("shortCodeLength")
    private int shortCodeLength;

    @JsonProperty("storageFile")
    private String storageFile;

    @JsonProperty("cleanupIntervalMinutes")
    private int cleanupIntervalMinutes = 60;

    @JsonProperty("enableAutoRedirect")
    private boolean enableAutoRedirect = true;

    @JsonProperty("dateTimeFormat")
    private String dateTimeFormat = "yyyy-MM-dd HH:mm";

    @JsonProperty("maxTTLDays")
    private int maxTTLDays = 365;

    public Config() {}

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getDefaultTTLHours() {
        return defaultTTLHours;
    }

    public void setDefaultTTLHours(int defaultTTLHours) {
        this.defaultTTLHours = defaultTTLHours;
    }

    public int getDefaultMaxClicks() {
        return defaultMaxClicks;
    }

    public void setDefaultMaxClicks(int defaultMaxClicks) {
        this.defaultMaxClicks = defaultMaxClicks;
    }

    public int getShortCodeLength() {
        return shortCodeLength;
    }

    public void setShortCodeLength(int shortCodeLength) {
        this.shortCodeLength = shortCodeLength;
    }

    public String getStorageFile() {
        return storageFile;
    }

    public void setStorageFile(String storageFile) {
        this.storageFile = storageFile;
    }

    public int getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }

    public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;
    }

    public boolean isEnableAutoRedirect() {
        return enableAutoRedirect;
    }

    public void setEnableAutoRedirect(boolean enableAutoRedirect) {
        this.enableAutoRedirect = enableAutoRedirect;
    }

    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }

    public int getMaxTTLDays() {
        return maxTTLDays;
    }

    public void setMaxTTLDays(int maxTTLDays) {
        this.maxTTLDays = maxTTLDays;
    }

    public static Config createDefault() {
        Config config = new Config();
        config.setBaseUrl("click.by/");
        config.setDefaultTTLHours(24);
        config.setDefaultMaxClicks(100);
        config.setShortCodeLength(6);
        config.setStorageFile("data/url_shortener_data.json");
        config.setCleanupIntervalMinutes(60);
        config.setEnableAutoRedirect(true);
        config.setMaxTTLDays(365);
        config.setDateTimeFormat("yyyy-MM-dd HH:mm");
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Config config = (Config) o;
        return defaultTTLHours == config.defaultTTLHours &&
                defaultMaxClicks == config.defaultMaxClicks &&
                shortCodeLength == config.shortCodeLength &&
                cleanupIntervalMinutes == config.cleanupIntervalMinutes &&
                enableAutoRedirect == config.enableAutoRedirect &&
                maxTTLDays == config.maxTTLDays &&
                Objects.equals(baseUrl, config.baseUrl) &&
                Objects.equals(storageFile, config.storageFile) &&
                Objects.equals(dateTimeFormat, config.dateTimeFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, defaultTTLHours, defaultMaxClicks,
                shortCodeLength, storageFile, cleanupIntervalMinutes,
                enableAutoRedirect, dateTimeFormat, maxTTLDays);
    }

    @Override
    public String toString() {
        return String.format(
                "Config{baseUrl='%s', TTL=%d часов, maxClicks=%d, codeLength=%d, " +
                        "storage='%s', maxTTL=%d дней}",
                baseUrl, defaultTTLHours, defaultMaxClicks,
                shortCodeLength, storageFile, maxTTLDays
        );
    }
}