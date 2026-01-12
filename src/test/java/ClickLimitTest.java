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

class ClickLimitTest {

    @Test
    void urlBlocksAfterMaxClicksReached() {
        System.out.println("🟡 ТЕСТ 2: Проверка блокировки при исчерпании лимита переходов");
        System.out.println("==============================================================");

        // Шаг 1: Подготовка тестовых данных
        System.out.println("✅ Шаг 1: Подготовка тестовых данных...");
        ShortCode testCode = new ShortCode("TEST123");
        Url testUrl = new Url("https://example.com");
        LocalDateTime now = LocalDateTime.now();

        System.out.println("   • Тестовый код: " + testCode.value());
        System.out.println("   • URL: " + testUrl.value());
        System.out.println("   • Установлен лимит: 3 перехода");

        // Шаг 2: Создание тестовой ссылки
        System.out.println("✅ Шаг 2: Создание тестовой ссылки с лимитом 3 перехода...");
        ShortenedUrl shortenedUrl = ShortenedUrl.createWithCustomExpiration(
                testUrl,
                testCode,
                UserId.generate(),
                now.plusHours(24),
                3 // Максимум 3 клика
        );

        // Шаг 3: Создание моков
        System.out.println("✅ Шаг 3: Создание и настройка моков...");
        UrlRepository urlRepository = mock(UrlRepository.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        UrlValidator urlValidator = mock(UrlValidator.class);
        DateTimeProvider dateTimeProvider = mock(DateTimeProvider.class);

        when(urlValidator.isValid(anyString())).thenReturn(true);
        when(dateTimeProvider.now()).thenReturn(now);
        when(dateTimeProvider.plusHours(anyLong())).thenReturn(now.plusHours(24));
        when(dateTimeProvider.plusDays(anyLong())).thenReturn(now.plusDays(365));

        // Настраиваем репозиторий возвращать нашу ссылку
        when(urlRepository.findByShortCode(testCode))
                .thenReturn(Optional.of(shortenedUrl));

        // Шаг 4: Создание сервиса
        System.out.println("✅ Шаг 4: Создание сервиса UrlShortenerService...");
        UrlShortenerServiceImpl service = new UrlShortenerServiceImpl(
                urlRepository, idGenerator, urlValidator, dateTimeProvider,
                24, 100, 6, 365
        );

        // Шаг 5: Тестирование переходов в пределах лимита
        System.out.println("✅ Шаг 5: Тестирование переходов в пределах лимита...");
        System.out.println("   • Переход 1/3...");
        assertDoesNotThrow(() -> service.redirect(testCode),
                "Первый переход должен быть успешным");
        System.out.println("     ✓ Успешно");

        System.out.println("   • Переход 2/3...");
        assertDoesNotThrow(() -> service.redirect(testCode),
                "Второй переход должен быть успешным");
        System.out.println("     ✓ Успешно");

        System.out.println("   • Переход 3/3...");
        assertDoesNotThrow(() -> service.redirect(testCode),
                "Третий переход должен быть успешным");
        System.out.println("     ✓ Успешно (лимит достигнут)");

        // Шаг 6: Тестирование блокировки при превышении лимита
        System.out.println("✅ Шаг 6: Тестирование блокировки при превышении лимита...");
        System.out.println("   • Попытка перехода 4/3 (превышение лимита)...");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.redirect(testCode),
                "При исчерпании лимита должно выбрасываться исключение"
        );

        System.out.println("     ✓ Исключение получено: " + exception.getMessage());

        // Шаг 7: Проверка сообщения об ошибке
        System.out.println("✅ Шаг 7: Проверка сообщения об ошибке...");
        String errorMessage = exception.getMessage().toLowerCase();
        boolean hasCorrectMessage = errorMessage.contains("лимит") ||
                errorMessage.contains("переход") ||
                errorMessage.contains("исчерпан") ||
                errorMessage.contains("недоступна");

        assertTrue(hasCorrectMessage,
                "Сообщение об ошибке должно указывать на исчерпание лимита переходов. Получено: " + exception.getMessage());

        System.out.println("     ✓ Сообщение корректно: \"" + exception.getMessage() + "\"");

        // Шаг 8: Итоги теста
        System.out.println("✅ Шаг 8: Тест пройден успешно!");
        System.out.println("   ✓ Первые 3 перехода выполнены успешно");
        System.out.println("   ✓ 4-й переход заблокирован с корректным сообщением");
        System.out.println("   ✓ Система корректно обрабатывает ограничение по кликам");
        System.out.println("==============================================================\n");
    }
}
