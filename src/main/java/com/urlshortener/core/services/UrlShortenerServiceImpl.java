package com.urlshortener.core.services;

import com.urlshortener.core.domain.exceptions.PermissionDeniedException;
import com.urlshortener.core.domain.exceptions.UrlNotFoundException;
import com.urlshortener.core.domain.exceptions.ValidationException;
import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.ports.input.UrlShortenerUseCase;
import com.urlshortener.core.ports.output.DateTimeProvider;
import com.urlshortener.core.ports.output.IdGenerator;
import com.urlshortener.core.ports.output.UrlRepository;
import com.urlshortener.core.ports.output.UrlValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Реализация Use Case для операций с короткими ссылками
 */
public class UrlShortenerServiceImpl implements UrlShortenerUseCase {

    private final UrlRepository urlRepository;
    private final IdGenerator idGenerator;
    private final UrlValidator urlValidator;
    private final DateTimeProvider dateTimeProvider;
    private final int defaultTTLHours;
    private final int defaultMaxClicks;
    private final int shortCodeLength;
    private final int maxTTLDays;

    public UrlShortenerServiceImpl(UrlRepository urlRepository,
                                   IdGenerator idGenerator,
                                   UrlValidator urlValidator,
                                   DateTimeProvider dateTimeProvider,
                                   int defaultTTLHours,
                                   int defaultMaxClicks,
                                   int shortCodeLength,
                                   int maxTTLDays) {
        this.urlRepository = urlRepository;
        this.idGenerator = idGenerator;
        this.urlValidator = urlValidator;
        this.dateTimeProvider = dateTimeProvider;
        this.defaultTTLHours = defaultTTLHours;
        this.defaultMaxClicks = defaultMaxClicks;
        this.shortCodeLength = shortCodeLength;
        this.maxTTLDays = maxTTLDays;
    }

    @Override
    public ShortenedUrl shortenUrl(Url originalUrl, UserId userId) {
        return shortenUrlWithExpirationAndClicks(
                originalUrl,
                userId,
                dateTimeProvider.now().plusHours(defaultTTLHours),
                defaultMaxClicks
        );
    }

    @Override
    public ShortenedUrl shortenUrl(Url originalUrl, UserId userId, int ttlHours) {
        validateTTL(ttlHours);

        return shortenUrlWithExpirationAndClicks(
                originalUrl,
                userId,
                dateTimeProvider.now().plusHours(ttlHours),
                defaultMaxClicks
        );
    }

    @Override
    public ShortenedUrl shortenUrlWithClicks(Url originalUrl, UserId userId, int maxClicks) {
        validateMaxClicks(maxClicks);

        return shortenUrlWithExpirationAndClicks(
                originalUrl,
                userId,
                dateTimeProvider.now().plusHours(defaultTTLHours),
                maxClicks
        );
    }

    @Override
    public ShortenedUrl shortenUrlWithExpiration(Url originalUrl, UserId userId,
                                                 LocalDateTime expiresAt) {
        validateExpirationDate(expiresAt);

        return shortenUrlWithExpirationAndClicks(
                originalUrl,
                userId,
                expiresAt,
                defaultMaxClicks
        );
    }

    @Override
    public ShortenedUrl shortenUrlWithExpirationAndClicks(Url originalUrl, UserId userId,
                                                          LocalDateTime expiresAt,
                                                          int maxClicks) {
        // Валидация входных данных
        validateUrl(originalUrl.value());
        validateExpirationDate(expiresAt);
        validateMaxClicks(maxClicks);

        // Проверка на дубликат
        checkForDuplicate(originalUrl, userId);

        // Генерация короткого кода
        ShortCode shortCode = idGenerator.generate(originalUrl, userId, shortCodeLength);

        // Создание доменного объекта
        ShortenedUrl url = ShortenedUrl.createWithCustomExpiration(
                originalUrl,
                shortCode,
                userId,
                expiresAt,
                maxClicks
        );

        // Сохранение
        return urlRepository.save(url);
    }

    @Override
    public Url redirect(ShortCode shortCode) {
        ShortenedUrl url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode.value()));

        // Проверка доступности ссылки
        if (!url.canBeAccessed()) {
            String message;
            if (url.isExpired()) {
                message = "Срок действия ссылки истек";
            } else if (url.getCurrentClicks() >= url.getMaxClicks()) {
                message = "Лимит переходов исчерпан";
            } else {
                message = "Ссылка заблокирована";
            }
            throw new IllegalStateException(message);
        }

        // Увеличиваем счетчик
        url.incrementClicks();
        urlRepository.save(url);

        return url.getOriginalUrl();
    }

    @Override
    public ShortenedUrl getUrlInfo(ShortCode shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode.value()));
    }

    @Override
    public void deleteUrl(ShortCode shortCode, UserId userId) {
        ShortenedUrl url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode.value()));

        // Проверка прав
        if (!url.getUserId().equals(userId)) {
            throw new PermissionDeniedException("У вас нет прав на удаление этой ссылки");
        }

        urlRepository.delete(shortCode);
    }

    @Override
    public ShortenedUrl editUrl(ShortCode shortCode, UserId userId,
                                Url newUrl, LocalDateTime newExpiresAt) {
        ShortenedUrl url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode.value()));

        // Проверка прав
        if (!url.getUserId().equals(userId)) {
            throw new PermissionDeniedException("У вас нет прав на редактирование этой ссылки");
        }

        boolean changesMade = false;

        // Обновление URL
        if (newUrl != null && !url.getOriginalUrl().equals(newUrl)) {
            validateUrl(newUrl.value());
            if (url.updateUrl(newUrl)) {
                changesMade = true;
            }
        }

        // Обновление даты истечения
        if (newExpiresAt != null && !url.getExpiresAt().equals(newExpiresAt)) {
            validateExpirationDate(newExpiresAt);
            LocalDateTime maxExpiresAt = dateTimeProvider.now().plusDays(maxTTLDays);
            if (url.updateExpiration(newExpiresAt, maxExpiresAt)) {
                changesMade = true;
            }
        }

        if (changesMade) {
            return urlRepository.save(url);
        }

        return url;
    }

    @Override
    public List<ShortenedUrl> getUserUrls(UserId userId) {
        return urlRepository.findByUserId(userId);
    }

    @Override
    public boolean urlExists(ShortCode shortCode) {
        return urlRepository.existsByShortCode(shortCode);
    }

    @Override
    public boolean hasPermission(ShortCode shortCode, UserId userId) {
        Optional<ShortenedUrl> url = urlRepository.findByShortCode(shortCode);
        return url.isPresent() && url.get().getUserId().equals(userId);
    }

    // Приватные методы валидации
    private void validateUrl(String url) {
        if (!urlValidator.isValid(url)) {
            throw new ValidationException("Некорректный URL: " + url);
        }
    }

    private void validateExpirationDate(LocalDateTime expiresAt) {
        if (expiresAt.isBefore(dateTimeProvider.now())) {
            throw new ValidationException("Дата истечения должна быть в будущем");
        }

        LocalDateTime maxExpiresAt = dateTimeProvider.now().plusDays(maxTTLDays);
        if (expiresAt.isAfter(maxExpiresAt)) {
            throw new ValidationException(
                    "Срок действия ссылки не может превышать " + maxTTLDays + " дней"
            );
        }
    }

    private void validateTTL(int ttlHours) {
        if (ttlHours <= 0) {
            throw new ValidationException("TTL должен быть положительным числом");
        }

        int maxHours = maxTTLDays * 24;
        if (ttlHours > maxHours) {
            throw new ValidationException(
                    "TTL не может превышать " + maxTTLDays + " дней"
            );
        }
    }

    private void validateMaxClicks(int maxClicks) {
        if (maxClicks <= 0) {
            throw new ValidationException("Количество переходов должно быть положительным числом");
        }
    }

    private void checkForDuplicate(Url originalUrl, UserId userId) {
        List<ShortenedUrl> userUrls = urlRepository.findByUserId(userId);

        for (ShortenedUrl existingUrl : userUrls) {
            if (existingUrl.getOriginalUrl().equals(originalUrl) &&
                    existingUrl.canBeAccessed()) {
                throw new ValidationException(
                        "У вас уже есть активная ссылка для этого URL"
                );
            }
        }
    }
}