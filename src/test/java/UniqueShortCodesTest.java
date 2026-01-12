import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.services.UrlShortenerServiceImpl;
import com.urlshortener.core.ports.output.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UniqueShortCodesTest {

    @Test
    void sameUrlDifferentUsersGeneratesDifferentShortCodes() {
        System.out.println("🟡 ТЕСТ 1: Проверка уникальности кодов для разных пользователей");
        System.out.println("==============================================================");

        // Шаг 1: Подготовка тестовых данных
        System.out.println("✅ Шаг 1: Подготовка тестовых данных...");
        Url testUrl = new Url("https://example.com");
        UserId user1 = UserId.generate();
        UserId user2 = UserId.generate();

        System.out.println("   • Тестовый URL: " + testUrl.value());
        System.out.println("   • Пользователь 1: " + user1.shortId() + "...");
        System.out.println("   • Пользователь 2: " + user2.shortId() + "...");

        // Шаг 2: Создание моков
        System.out.println("✅ Шаг 2: Создание моков зависимостей...");
        UrlRepository urlRepository = mock(UrlRepository.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        UrlValidator urlValidator = mock(UrlValidator.class);
        DateTimeProvider dateTimeProvider = mock(DateTimeProvider.class);

        // Шаг 3: Настройка моков
        System.out.println("✅ Шаг 3: Настройка поведения моков...");
        when(urlValidator.isValid(anyString())).thenReturn(true);

        LocalDateTime now = LocalDateTime.now();
        when(dateTimeProvider.now()).thenReturn(now);
        when(dateTimeProvider.plusHours(anyLong())).thenReturn(now.plusHours(24));
        when(dateTimeProvider.plusDays(anyLong())).thenReturn(now.plusDays(365));

        // Устанавливаем разные коды для разных пользователей
        when(idGenerator.generate(eq(testUrl), eq(user1), anyInt()))
                .thenReturn(new com.urlshortener.core.domain.valueobjects.ShortCode("ABC123"));
        when(idGenerator.generate(eq(testUrl), eq(user2), anyInt()))
                .thenReturn(new com.urlshortener.core.domain.valueobjects.ShortCode("XYZ789"));

        // Настраиваем репозиторий
        when(urlRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(urlRepository.findByUserId(any())).thenReturn(new ArrayList<>());

        // Шаг 4: Создание сервиса
        System.out.println("✅ Шаг 4: Создание сервиса UrlShortenerService...");
        UrlShortenerServiceImpl service = new UrlShortenerServiceImpl(
                urlRepository,
                idGenerator,
                urlValidator,
                dateTimeProvider,
                24, // defaultTTLHours
                100, // defaultMaxClicks
                6, // shortCodeLength
                365 // maxTTLDays
        );

        // Шаг 5: Создание коротких ссылок
        System.out.println("✅ Шаг 5: Создание коротких ссылок для двух пользователей...");
        ShortenedUrl url1 = service.shortenUrl(testUrl, user1);
        System.out.println("   • Создана ссылка для пользователя 1: " + "click.by/" +
                url1.getShortCode().value() + " → " + testUrl.value());

        ShortenedUrl url2 = service.shortenUrl(testUrl, user2);
        System.out.println("   • Создана ссылка для пользователя 2: " + "click.by/" +
                url2.getShortCode().value() + " → " + testUrl.value());

        // Шаг 6: Проверка результатов
        System.out.println("✅ Шаг 6: Проверка уникальности сгенерированных кодов...");
        System.out.println("   • Код пользователя 1: " + url1.getShortCode().value());
        System.out.println("   • Код пользователя 2: " + url2.getShortCode().value());

        // Проверяем что коды разные
        assertNotEquals(
                url1.getShortCode().value(),
                url2.getShortCode().value(),
                "Одна и та же ссылка для разных пользователей должна генерировать разные коды"
        );

        // Проверяем конкретные значения
        assertEquals("ABC123", url1.getShortCode().value(),
                "Первый пользователь должен получить код ABC123");
        assertEquals("XYZ789", url2.getShortCode().value(),
                "Второй пользователь должен получить код XYZ789");

        // Шаг 7: Итоги теста
        System.out.println("✅ Шаг 7: Тест пройден успешно!");
        System.out.println("   ✓ Коды уникальны: " + url1.getShortCode().value() + " != " + url2.getShortCode().value());
        System.out.println("   ✓ Оба кода соответствуют ожидаемым значениям");
        System.out.println("==============================================================\n");
    }
}
