package com.urlshortener.core.services;

import com.urlshortener.core.domain.models.Config;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.ports.input.StatisticsUseCase;
import com.urlshortener.core.ports.output.UrlRepository;
import com.urlshortener.core.ports.output.UserRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Реализация для статистики
 */
public class StatisticsServiceImpl implements StatisticsUseCase {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final Config config;

    public StatisticsServiceImpl(UrlRepository urlRepository,
                                 UserRepository userRepository,
                                 Config config) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
        this.config = config;
    }

    @Override
    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUrls", urlRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUrls", urlRepository.countActive());
        stats.put("expiredUrls", urlRepository.countExpired());

        return stats;
    }

    @Override
    public Map<String, Object> getUserStatistics(UserId userId) {
        Map<String, Object> stats = new HashMap<>();

        var userUrls = urlRepository.findByUserId(userId);

        long totalUrls = userUrls.size();
        long totalClicks = userUrls.stream()
                .mapToInt(url -> url.getCurrentClicks())
                .sum();
        long activeUrls = userUrls.stream()
                .filter(url -> url.canBeAccessed())
                .count();

        stats.put("totalUrls", totalUrls);
        stats.put("totalClicks", totalClicks);
        stats.put("activeUrls", activeUrls);

        return stats;
    }

    @Override
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> configInfo = new HashMap<>();

        configInfo.put("baseUrl", config.getBaseUrl());
        configInfo.put("defaultTTLHours", config.getDefaultTTLHours());
        configInfo.put("defaultMaxClicks", config.getDefaultMaxClicks());
        configInfo.put("shortCodeLength", config.getShortCodeLength());
        configInfo.put("storageFile", config.getStorageFile());
        configInfo.put("cleanupIntervalMinutes", config.getCleanupIntervalMinutes());
        configInfo.put("enableAutoRedirect", config.isEnableAutoRedirect());
        configInfo.put("dateTimeFormat", config.getDateTimeFormat());
        configInfo.put("maxTTLDays", config.getMaxTTLDays());

        return configInfo;
    }
}