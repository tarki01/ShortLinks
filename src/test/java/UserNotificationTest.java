import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.services.UrlShortenerServiceImpl;
import com.urlshortener.core.ports.output.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserNotificationTest {

    // Вспомогательный метод для создания ссылки с любой датой
    private ShortenedUrl createUrlWithAnyExpiration(Url originalUrl, ShortCode shortCode,
                                                    UserId userId, LocalDateTime expiresAt,
                                                    int maxClicks, int currentClicks, boolean active) {
        return new ShortenedUrl(
                originalUrl,
                shortCode,
                userId,
                LocalDateTime.now().minusHours(2),
                expiresAt,
                maxClicks,
                currentClicks,
                active
        );
    }

    @Test
    void userGetsClearNotificationWhenUrlUnavailable() {
        System.out.println("🟡 ТЕСТ 4: Проверка уведомлений о недоступности ссылок");
        System.out.println("==============================================================");

        // Шаг 1: Подготовка инфраструктуры
        System.out.println("✅ Шаг 1: Подготовка инфраструктуры и моков...");
        UrlRepository urlRepository = mock(UrlRepository.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        UrlValidator urlValidator = mock(UrlValidator.class);
        DateTimeProvider dateTimeProvider = mock(DateTimeProvider.class);

        LocalDateTime now = LocalDateTime.now();
        System.out.println("   • Текущее время: " + now);

        when(dateTimeProvider.now()).thenReturn(now);
        when(dateTimeProvider.plusHours(anyLong())).thenReturn(now.plusHours(24));
        when(dateTimeProvider.plusDays(anyLong())).thenReturn(now.plusDays(365));

        UrlShortenerServiceImpl service = new UrlShortenerServiceImpl(
                urlRepository, idGenerator, urlValidator, dateTimeProvider,
                24, 100, 6, 365
        );

        // ========== ТЕСТ 1: ИСЧЕРПАННЫЙ ЛИМИТ ПЕРЕХОДОВ ==========
        System.out.println("\n✅ Шаг 2: Тест 1 - Исчерпанный лимит переходов");
        ShortCode testCode = new ShortCode("TEST456");
        System.out.println("   • Тестируемая ссылка: " + "click.by/" + testCode.value());

        ShortenedUrl clicksExhausted = createUrlWithAnyExpiration(
                new Url("https://clicks-exhausted.com"),
                testCode,
                UserId.generate(),
                now.plusHours(24), // активна по времени
                3, // Максимум 3 клика
                3, // Уже 3 клика (лимит исчерпан)
                false // Ссылка неактивна
        );

        System.out.println("   • Параметры ссылки:");
        System.out.println("     - Макс. переходов: " + clicksExhausted.getMaxClicks());
        System.out.println("     - Текущих переходов: " + clicksExhausted.getCurrentClicks());
        System.out.println("     - Активна: " + clicksExhausted.canBeAccessed());

        when(urlRepository.findByShortCode(testCode))
                .thenReturn(Optional.of(clicksExhausted));

        // Проверка исключения
        System.out.println("   • Попытка перехода по ссылке с исчерпанным лимитом...");
        IllegalStateException clicksException = assertThrows(
                IllegalStateException.class,
                () -> service.redirect(testCode),
                "Должно быть исключение при исчерпании лимита"
        );

        System.out.println("     ✓ Получено исключение: \"" + clicksException.getMessage() + "\"");

        // Проверка сообщения
        String clicksMessage = clicksException.getMessage().toLowerCase();
        boolean hasCorrectClicksMessage = clicksMessage.contains("лимит") ||
                clicksMessage.contains("переход") ||
                clicksMessage.contains("исчерпан") ||
                clicksMessage.contains("недоступна");

        assertTrue(hasCorrectClicksMessage,
                "Сообщение должно указывать на исчерпание лимита переходов. Получено: " + clicksException.getMessage());
        System.out.println("     ✓ Сообщение корректно указывает на исчерпание лимита");

        // ========== ТЕСТ 2: ПРОСРОЧЕННАЯ ССЫЛКА ==========
        System.out.println("\n✅ Шаг 3: Тест 2 - Просроченная ссылка");
        ShortCode expiredCode = new ShortCode("EXPIRED");
        System.out.println("   • Тестируемая ссылка: " + "click.by/" + expiredCode.value());

        ShortenedUrl expiredUrl = createUrlWithAnyExpiration(
                new Url("https://expired.com"),
                expiredCode,
                UserId.generate(),
                now.minusHours(1), // Истекла час назад
                100,
                0,
                false
        );

        System.out.println("   • Параметры ссылки:");
        System.out.println("     - Истекла: " + expiredUrl.getExpiresAt());
        System.out.println("     - Активна: " + expiredUrl.canBeAccessed());
        System.out.println("     - isExpired(): " + expiredUrl.isExpired());

        when(urlRepository.findByShortCode(expiredCode))
                .thenReturn(Optional.of(expiredUrl));

        // Проверка исключения
        System.out.println("   • Попытка перехода по просроченной ссылке...");
        IllegalStateException expiredException = assertThrows(
                IllegalStateException.class,
                () -> service.redirect(expiredCode),
                "Должно быть исключение для просроченной ссылки"
        );

        System.out.println("     ✓ Получено исключение: \"" + expiredException.getMessage() + "\"");

        // Проверка сообщения
        String expiredMessage = expiredException.getMessage().toLowerCase();
        boolean hasCorrectExpiredMessage = expiredMessage.contains("истек") ||
                expiredMessage.contains("срок") ||
                expiredMessage.contains("действия") ||
                expiredMessage.contains("недоступна");

        assertTrue(hasCorrectExpiredMessage,
                "Сообщение должно указывать на истечение срока. Получено: " + expiredException.getMessage());
        System.out.println("     ✓ Сообщение корректно указывает на истечение срока");

        // ========== ТЕСТ 3: ЗАБЛОКИРОВАННАЯ ССЫЛКА ==========
        System.out.println("\n✅ Шаг 4: Тест 3 - Заблокированная ссылка");
        ShortCode blockedCode = new ShortCode("BLOCKED");
        System.out.println("   • Тестируемая ссылка: " + "click.by/" + blockedCode.value());

        ShortenedUrl blockedUrl = createUrlWithAnyExpiration(
                new Url("https://blocked.com"),
                blockedCode,
                UserId.generate(),
                now.plusHours(24), // Не истекла
                100,
                0,
                false // Явно заблокирована
        );

        System.out.println("   • Параметры ссылки:");
        System.out.println("     - Истекает: " + blockedUrl.getExpiresAt());
        System.out.println("     - Активна (флаг): " + blockedUrl.canBeAccessed());
        System.out.println("     - Блокировка: явная (active=false)");

        when(urlRepository.findByShortCode(blockedCode))
                .thenReturn(Optional.of(blockedUrl));

        // Проверка исключения
        System.out.println("   • Попытка перехода по заблокированной ссылке...");
        IllegalStateException blockedException = assertThrows(
                IllegalStateException.class,
                () -> service.redirect(blockedCode),
                "Должно быть исключение для заблокированной ссылки"
        );

        System.out.println("     ✓ Получено исключение: \"" + blockedException.getMessage() + "\"");

        // Проверка сообщения
        String blockedMessage = blockedException.getMessage().toLowerCase();
        boolean hasCorrectBlockedMessage = blockedMessage.contains("блокиров") ||
                blockedMessage.contains("недоступ") ||
                blockedMessage.contains("заблокирована");

        assertTrue(hasCorrectBlockedMessage,
                "Сообщение должно указывать на блокировку. Получено: " + blockedException.getMessage());
        System.out.println("     ✓ Сообщение корректно указывает на блокировку");

        // ========== ИТОГИ ТЕСТА ==========
        System.out.println("\n✅ Шаг 5: Итоги теста");
        System.out.println("   ✓ Протестировано 3 различных сценария недоступности:");
        System.out.println("     1. Исчерпание лимита переходов ✓");
        System.out.println("     2. Истечение срока действия ✓");
        System.out.println("     3. Явная блокировка ссылки ✓");
        System.out.println("   ✓ Для каждого сценария получено корректное сообщение об ошибке");
        System.out.println("   ✓ Пользователь получает понятные уведомления о причине недоступности");
        System.out.println("==============================================================\n");
    }
}
