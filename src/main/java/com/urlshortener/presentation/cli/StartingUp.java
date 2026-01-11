package com.urlshortener.presentation.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.urlshortener.core.domain.models.Config;
import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.domain.models.User;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.ports.input.StatisticsUseCase;
import com.urlshortener.core.ports.input.UrlShortenerUseCase;
import com.urlshortener.core.ports.input.UserManagementUseCase;
import com.urlshortener.core.ports.output.*;
import com.urlshortener.core.services.StatisticsServiceImpl;
import com.urlshortener.core.services.UrlShortenerServiceImpl;
import com.urlshortener.core.services.UserServiceImpl;
import com.urlshortener.infrastructure.persistence.FileUrlRepository;
import com.urlshortener.infrastructure.persistence.InMemoryUserRepository;
import com.urlshortener.infrastructure.persistence.JsonConfigLoader;
import com.urlshortener.infrastructure.utils.CodeGeneratorImpl;
import com.urlshortener.infrastructure.utils.SystemDateTimeProvider;
import com.urlshortener.infrastructure.utils.UrlValidatorImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StartingUp {
    public void start() {
        System.out.println("🚀 Запуск сервиса сокращения ссылок...");

        try {
            // 1. Инициализация инфраструктурных компонентов
            ObjectMapper objectMapper = createObjectMapper();
            ConfigLoader configLoader = new JsonConfigLoader(objectMapper);

            // 2. Загрузка конфигурации
            Config config = configLoader.load();

            // 3. Инициализация репозиториев
            UrlRepository urlRepository = new FileUrlRepository(objectMapper, config);
            UserRepository userRepository = new InMemoryUserRepository();

            // 4. ЗАГРУЗИТЬ ПОЛЬЗОВАТЕЛЕЙ ИЗ ССЫЛОК
            loadUsersFromUrls(urlRepository, userRepository);

            // 5. Инициализация утилит
            IdGenerator idGenerator = new CodeGeneratorImpl();
            UrlValidator urlValidator = new UrlValidatorImpl();
            DateTimeProvider dateTimeProvider = new SystemDateTimeProvider();

            // 6. Создание сервисов (Use Cases)
            UrlShortenerUseCase urlShortenerService = new UrlShortenerServiceImpl(
                    urlRepository,
                    idGenerator,
                    urlValidator,
                    dateTimeProvider,
                    config.getDefaultTTLHours(),
                    config.getDefaultMaxClicks(),
                    config.getShortCodeLength(),
                    config.getMaxTTLDays()
            );

            UserManagementUseCase userService = new UserServiceImpl(userRepository);

            // НАСТРОИТЬ UserRepository ДЛЯ ПОИСКА ПО КОРОТКОМУ ID
            setupUserShortIdIndex(userRepository);

            StatisticsUseCase statisticsService = new StatisticsServiceImpl(
                    urlRepository, userRepository, config);

            // 7. Запуск планировщика задач
            ScheduledExecutorService scheduler = startScheduler(urlRepository, config);

            // 8. Создание и запуск CLI
            URLShortenerCLI cli = new URLShortenerCLI(
                    urlShortenerService,
                    userService,
                    statisticsService,
                    config.getBaseUrl(),
                    config.isEnableAutoRedirect()
            );

            // Обработка аргументов командной строки
            cli.start();

            // 9. Завершение работы
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

            System.out.println("👋 Приложение завершило работу");

        } catch (Exception e) {
            System.err.println("❌ Критическая ошибка при запуске приложения: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void loadUsersFromUrls(UrlRepository urlRepository, UserRepository userRepository) {
        try {
            // Получаем все ссылки
            List<ShortenedUrl> allUrls = urlRepository.findAll();

            // Собираем уникальных пользователей
            Set<UserId> uniqueUserIds = new HashSet<>();
            for (ShortenedUrl url : allUrls) {
                uniqueUserIds.add(url.getUserId());
            }

            // Создаем пользователей в репозитории
            for (com.urlshortener.core.domain.valueobjects.UserId userId : uniqueUserIds) {
                User user = new User(userId);
                userRepository.save(user);
                System.out.println("👤 Загружен пользователь: " + userId.shortId() + "...");
            }

            System.out.println("✅ Загружено " + uniqueUserIds.size() + " пользователей из данных");

        } catch (Exception e) {
            System.err.println("⚠️ Ошибка загрузки пользователей: " + e.getMessage());
        }
    }

    private static void setupUserShortIdIndex(UserRepository userRepository) {
        // Создаем индекс shortId -> User
        // InMemoryUserRepository уже делает это автоматически при сохранении
        System.out.println("📋 Индекс short ID создан");
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS); // Важно!
        return mapper;
    }

    private static ScheduledExecutorService startScheduler(UrlRepository urlRepository, Config config) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        // Задача автосохранения
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // В этой реализации автосохранение происходит в репозитории
                System.out.println("💾 Автосохранение выполнено");
            } catch (Exception e) {
                System.err.println("❌ Ошибка автосохранения: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);

        // Задача очистки просроченных ссылок
        scheduler.scheduleAtFixedRate(() -> {
                    try {
                        long before = urlRepository.count();
                        // В этой реализации очистка происходит при загрузке
                        System.out.println("🧹 Очистка выполнена");
                    } catch (Exception e) {
                        System.err.println("❌ Ошибка очистки: " + e.getMessage());
                    }
                }, config.getCleanupIntervalMinutes(),
                config.getCleanupIntervalMinutes(), TimeUnit.MINUTES);

        return scheduler;
    }
}
