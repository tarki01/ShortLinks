import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.infrastructure.persistence.FileUrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.urlshortener.core.domain.models.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExpirationCleanupTest {

    @TempDir
    Path tempDir;

    // Вспомогательный метод для создания ссылки с любой датой
    private ShortenedUrl createUrlWithAnyExpiration(Url originalUrl, ShortCode shortCode,
                                                    UserId userId, LocalDateTime expiresAt,
                                                    int maxClicks) {
        return new ShortenedUrl(
                originalUrl,
                shortCode,
                userId,
                LocalDateTime.now().minusHours(1),
                expiresAt,
                maxClicks,
                0,
                true
        );
    }

    @Test
    void expiredUrlsAreNotAccessible() throws Exception {
        System.out.println("🟡 ТЕСТ 3: Проверка удаления/недоступности просроченных ссылок");
        System.out.println("==============================================================");

        // Шаг 1: Подготовка инфраструктуры
        System.out.println("✅ Шаг 1: Подготовка инфраструктуры...");
        Config config = Config.createDefault();
        String storagePath = tempDir.resolve("test_data.json").toString();
        config.setStorageFile(storagePath);

        System.out.println("   • Файл хранилища: " + storagePath);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        FileUrlRepository repository = new FileUrlRepository(objectMapper, config);

        LocalDateTime now = LocalDateTime.now();
        System.out.println("   • Текущее время: " + now);

        // Шаг 2: Создание тестовых ссылок с разными сроками
        System.out.println("✅ Шаг 2: Создание тестовых ссылок...");

        // 1. Истекшая час назад
        ShortenedUrl expired1Hour = createUrlWithAnyExpiration(
                new Url("https://expired-1h.com"),
                new ShortCode("EXP1H"),
                UserId.generate(),
                now.minusHours(1), // Истекла час назад
                100
        );
        System.out.println("   • Создана ссылка 'click.by/EXP1H': истекла час назад");

        // 2. Истекшая сутки назад
        ShortenedUrl expired24Hours = createUrlWithAnyExpiration(
                new Url("https://expired-24h.com"),
                new ShortCode("EXP24H"),
                UserId.generate(),
                now.minusDays(1), // Истекла сутки назад
                100
        );
        System.out.println("   • Создана ссылка 'click.by/EXP24H': истекла сутки назад");

        // 3. Активная (истекает через час)
        ShortenedUrl active = createUrlWithAnyExpiration(
                new Url("https://active.com"),
                new ShortCode("ACTIVE"),
                UserId.generate(),
                now.plusHours(1), // Истекает через час
                100
        );
        System.out.println("   • Создана ссылка 'click.by/ACTIVE': истекает через час");

        // Шаг 3: Сохранение ссылок в репозиторий
        System.out.println("✅ Шаг 3: Сохранение ссылок в репозиторий...");
        repository.save(expired1Hour);
        repository.save(expired24Hours);
        repository.save(active);
        System.out.println("   • Сохранено 3 ссылки в репозиторий");

        // Шаг 4: Проверка доступности ссылок
        System.out.println("✅ Шаг 4: Проверка доступности ссылок...");
        boolean canAccessExpired1H = expired1Hour.canBeAccessed();
        boolean canAccessExpired24H = expired24Hours.canBeAccessed();
        boolean canAccessActive = active.canBeAccessed();

        System.out.println("   • Доступность click.by/EXP1H (истек час): " + canAccessExpired1H + " (ожидается: false)");
        System.out.println("   • Доступность click.by/EXP24H (истекли сутки): " + canAccessExpired24H + " (ожидается: false)");
        System.out.println("   • Доступность click.by/ACTIVE (активна): " + canAccessActive + " (ожидается: true)");

        // Проверяем утверждения
        assertFalse(canAccessExpired1H, "Ссылка, истекшая час назад, должна быть недоступна");
        assertFalse(canAccessExpired24H, "Ссылка, истекшая сутки назад, должна быть недоступна");
        assertTrue(canAccessActive, "Активная ссылка должна быть доступна");

        // Шаг 5: Проверка метода isExpired
        System.out.println("✅ Шаг 5: Проверка метода isExpired()...");
        boolean isExpired1H = expired1Hour.isExpired();
        boolean isExpired24H = expired24Hours.isExpired();
        boolean isExpiredActive = active.isExpired();

        System.out.println("   • isExpired(click.by/EXP1H): " + isExpired1H + " (ожидается: true)");
        System.out.println("   • isExpired(click.by/EXP24H): " + isExpired24H + " (ожидается: true)");
        System.out.println("   • isExpired(click.by/ACTIVE): " + isExpiredActive + " (ожидается: false)");

        assertTrue(isExpired1H, "Метод isExpired должен возвращать true для истекшей ссылки");
        assertTrue(isExpired24H, "Метод isExpired должен возвращать true для истекшей ссылки");
        assertFalse(isExpiredActive, "Метод isExpired должен возвращать false для активной ссылки");

        // Шаг 6: Проверка поиска в репозитории
        System.out.println("✅ Шаг 6: Проверка поиска ссылок в репозитории...");
        Optional<ShortenedUrl> foundExpired = repository.findByShortCode(new ShortCode("EXP1H"));
        Optional<ShortenedUrl> foundActive = repository.findByShortCode(new ShortCode("ACTIVE"));

        System.out.println("   • Найдена ссылка click.by/EXP1H: " + foundExpired.isPresent() + " (ожидается: true)");
        System.out.println("   • Найдена ссылка click.by/ACTIVE: " + foundActive.isPresent() + " (ожидается: true)");

        assertTrue(foundExpired.isPresent(), "Репо должно находить истекшие ссылки");
        assertTrue(foundActive.isPresent(), "Репо должно находить активные ссылки");

        // Шаг 7: Проверка счетчиков репозитория
        System.out.println("✅ Шаг 7: Проверка счетчиков репозитория...");
        long totalCount = repository.count();
        System.out.println("   • Всего ссылок в репозитории: " + totalCount + " (ожидается: 3)");
        assertEquals(3, totalCount, "Всего должно быть 3 ссылки");

        // Шаг 8: Удаление истекших ссылок
        System.out.println("✅ Шаг 8: Удаление истекших ссылок...");
        repository.delete(new ShortCode("EXP1H"));
        repository.delete(new ShortCode("EXP24H"));
        System.out.println("   • Удалены ссылки click.by/EXP1H и click.by/EXP24H");

        // Шаг 9: Проверка после удаления
        System.out.println("✅ Шаг 9: Проверка состояния после удаления...");
        long countAfterDeletion = repository.count();
        System.out.println("   • Ссылок после удаления: " + countAfterDeletion + " (ожидается: 1)");

        boolean exp1hExists = repository.findByShortCode(new ShortCode("EXP1H")).isPresent();
        boolean exp24hExists = repository.findByShortCode(new ShortCode("EXP24H")).isPresent();
        boolean activeExists = repository.findByShortCode(new ShortCode("ACTIVE")).isPresent();

        System.out.println("   • click.by/EXP1H существует после удаления: " + exp1hExists + " (ожидается: false)");
        System.out.println("   • click.by/EXP24H существует после удаления: " + exp24hExists + " (ожидается: false)");
        System.out.println("   • click.by/ACTIVE существует после удаления: " + activeExists + " (ожидается: true)");

        assertEquals(1, countAfterDeletion, "После удаления должна остаться 1 ссылка");
        assertFalse(exp1hExists, "Удаленная ссылка не должна находиться в репозитории");
        assertFalse(exp24hExists, "Удаленная ссылка не должна находиться в репозитории");
        assertTrue(activeExists, "Активная ссылка должна остаться в репозитории");

        // Шаг 10: Итоги теста
        System.out.println("✅ Шаг 10: Тест пройден успешно!");
        System.out.println("   ✓ Ссылки корректно определяются как истекшие/активные");
        System.out.println("   ✓ Метод canBeAccessed() работает правильно");
        System.out.println("   ✓ Репозиторий корректно управляет ссылками");
        System.out.println("   ✓ Удаление работает корректно");
        System.out.println("==============================================================\n");
    }
}
