package com.urlshortener.presentation.cli;

import com.urlshortener.core.domain.models.ShortenedUrl;
import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.ports.input.StatisticsUseCase;
import com.urlshortener.core.ports.input.UrlShortenerUseCase;
import com.urlshortener.core.ports.input.UserManagementUseCase;
import com.urlshortener.core.services.UserServiceImpl;
import com.urlshortener.infrastructure.security.UserValidator;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Консольный интерфейс для сервиса сокращения ссылок
 */
public class URLShortenerCLI {

    private final UrlShortenerUseCase urlShortenerUseCase;
    private final UserManagementUseCase userManagementUseCase;
    private final StatisticsUseCase statisticsUseCase;
    private final CommandParser commandParser;
    private final UrlPrinter urlPrinter;
    private final Scanner scanner;
    private final String baseUrl;
    private final boolean enableAutoRedirect;

    private boolean isRunning;

    public URLShortenerCLI(UrlShortenerUseCase urlShortenerUseCase,
                           UserManagementUseCase userManagementUseCase,
                           StatisticsUseCase statisticsUseCase,
                           String baseUrl,
                           boolean enableAutoRedirect) {
        this.urlShortenerUseCase = urlShortenerUseCase;
        this.userManagementUseCase = userManagementUseCase;
        this.statisticsUseCase = statisticsUseCase;
        this.commandParser = new CommandParser();
        this.urlPrinter = new UrlPrinter();
        this.scanner = new Scanner(System.in);
        this.baseUrl = baseUrl;
        this.enableAutoRedirect = enableAutoRedirect;
        this.isRunning = true;
    }

    public void start() {
        urlPrinter.printBanner();
        initializeUser();
        urlPrinter.printHelp();

        runInteractiveMode();
    }

    private void initializeUser() {
        urlPrinter.printInfo("👤 ИНИЦИАЛИЗАЦИЯ ПОЛЬЗОВАТЕЛЯ");
        System.out.println("1. Использовать существующий UUID");
        System.out.println("2. Создать нового пользователя");
        System.out.print("Выберите вариант [1/2]: ");

        String choice = scanner.nextLine().trim();

        if ("1".equals(choice)) {
            System.out.print("Введите ваш UUID (полный или первые 8 символов): ");
            String userIdStr = scanner.nextLine().trim();

            if (userManagementUseCase instanceof UserServiceImpl) {
                try {
                    ((UserServiceImpl) userManagementUseCase).switchUser(userIdStr);
                    var currentUser = userManagementUseCase.getCurrentUser()
                            .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
                    urlPrinter.printSuccess("Вошли как пользователь: " + currentUser.getShortId() + "...");
                } catch (IllegalArgumentException e) {
                    urlPrinter.printError(e.getMessage());
                    urlPrinter.printWarning("Создаю нового пользователя...");
                    userManagementUseCase.createUser();
                }
            } else {
                // Fallback
                try {
                    UserId userId = UserId.fromString(userIdStr);
                    userManagementUseCase.switchUser(userId);
                    urlPrinter.printSuccess("Вошли как пользователь: " + userId.shortId() + "...");
                } catch (IllegalArgumentException e) {
                    urlPrinter.printError(e.getMessage());
                    urlPrinter.printWarning("Создаю нового пользователя...");
                    userManagementUseCase.createUser();
                }
            }
        } else {
            userManagementUseCase.createUser();
        }
    }

    private void runInteractiveMode() {
        while (isRunning) {
            try {
                var currentUser = userManagementUseCase.getCurrentUser();
                String prompt = currentUser
                        .map(user -> ConsoleColors.bold(ConsoleColors.cyan(
                                "🔗 " + user.getShortId() + "... > ")))
                        .orElse(ConsoleColors.bold(ConsoleColors.cyan("🔗 > ")));

                System.out.println();
                System.out.print(prompt);

                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }

                processCommand(input);

            } catch (NoSuchElementException e) {
                // Ctrl+D или конец ввода
                urlPrinter.printWarning("Завершение работы (конец ввода)...");
                shutdown();
                break;
            } catch (Exception e) {
                urlPrinter.printError("Непредвиденная ошибка: " + e.getMessage());
            }
        }
    }

    public void processCommand(String input) {
        CommandParser.ParsedCommand parsed = commandParser.parse(input);

        try {
            switch (parsed.getType()) {
                case SHORTEN -> handleShorten(parsed);
                case GO -> handleGo(parsed);
                case LIST -> handleList();
                case INFO -> handleInfo(parsed);
                case EDIT -> handleEdit(parsed);
                case DELETE -> handleDelete(parsed);
                case SWITCH -> handleSwitch(parsed);
                case NEWUSER -> handleNewUser();
                case WHOAMI -> handleWhoAmI();
                case STATS -> handleStats();
                case CONFIG -> handleConfig();
                case HELP -> urlPrinter.printHelp();
                case EXIT -> shutdown();
                default -> urlPrinter.printError("Неизвестная команда. Введите 'help' для справки");
            }
        } catch (Exception e) {
            urlPrinter.printError(e.getMessage());
        }
    }

    private void handleShorten(CommandParser.ParsedCommand parsed) {
        if (parsed.getArgCount() < 1) {
            urlPrinter.printError("Используйте: shorten <url> [дата/часы] [переходы]");
            return;
        }

        var currentUser = userManagementUseCase.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        try {
            // Разбор параметров
            Url originalUrl = new Url(parsed.getArg(0));
            LocalDateTime expiresAt = null;
            Integer maxClicks = null;
            Integer ttlHours = null;

            // Упрощенная логика парсинга (как в оригинале)
            if (parsed.getArgCount() >= 2) {
                String param2 = parsed.getArg(1);

                if (parsed.getArgCount() == 2) {
                    // Два параметра: shorten <url> <число> или shorten <url> <дата>
                    if (param2.matches("^\\d+$")) {
                        maxClicks = commandParser.parseInteger(param2, "Количество переходов");
                    } else {
                        expiresAt = commandParser.parseDateTime(param2);
                    }
                } else if (parsed.getArgCount() == 3) {
                    String param3 = parsed.getArg(2);

                    if (param2.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                        // Дата + переходы или дата + время
                        if (param3.matches("^\\d{2}:\\d{2}$")) {
                            expiresAt = commandParser.parseDateTime(param2 + " " + param3);
                        } else if (param3.matches("^\\d+$")) {
                            expiresAt = commandParser.parseDateTime(param2 + " 23:59");
                            maxClicks = commandParser.parseInteger(param3, "Количество переходов");
                        }
                    } else if (param2.matches("^\\d+$") && param3.matches("^\\d+$")) {
                        // Часы + переходы
                        ttlHours = commandParser.parseInteger(param2, "Количество часов");
                        maxClicks = commandParser.parseInteger(param3, "Количество переходов");
                    }
                } else if (parsed.getArgCount() >= 4) {
                    // Дата + время + переходы
                    String dateStr = parsed.getArg(1);
                    String timeStr = parsed.getArg(2);
                    String clicksStr = parsed.getArg(3);

                    expiresAt = commandParser.parseDateTime(dateStr + " " + timeStr);
                    maxClicks = commandParser.parseInteger(clicksStr, "Количество переходов");
                }
            }

            // Создание ссылки
            ShortenedUrl shortenedUrl;

            if (expiresAt != null) {
                if (maxClicks != null) {
                    shortenedUrl = urlShortenerUseCase.shortenUrlWithExpirationAndClicks(
                            originalUrl, currentUser.getId(), expiresAt, maxClicks);
                } else {
                    shortenedUrl = urlShortenerUseCase.shortenUrlWithExpiration(
                            originalUrl, currentUser.getId(), expiresAt);
                }
            } else if (ttlHours != null) {
                if (maxClicks != null) {
                    shortenedUrl = urlShortenerUseCase.shortenUrl(
                            originalUrl, currentUser.getId(), ttlHours);
                    // Note: оригинальный сервис использовал ttlHours для часов, но без maxClicks параметра
                    // В этой версии просто используем дефолтные клики
                } else {
                    shortenedUrl = urlShortenerUseCase.shortenUrl(
                            originalUrl, currentUser.getId(), ttlHours);
                }
            } else {
                if (maxClicks != null) {
                    shortenedUrl = urlShortenerUseCase.shortenUrlWithClicks(
                            originalUrl, currentUser.getId(), maxClicks);
                } else {
                    shortenedUrl = urlShortenerUseCase.shortenUrl(
                            originalUrl, currentUser.getId());
                }
            }

            urlPrinter.printSuccess("Создана короткая ссылка: " +
                    shortenedUrl.getShortUrl(baseUrl));

        } catch (IllegalArgumentException e) {
            urlPrinter.printError(e.getMessage());
        }
    }

    private void handleGo(CommandParser.ParsedCommand parsed) {
        if (parsed.getArgCount() < 1) {
            urlPrinter.printError("Используйте: go <короткая_ссылка>");
            return;
        }

        try {
            ShortCode shortCode = ShortCode.fromShortUrl(parsed.getArg(0), baseUrl);
            Url originalUrl = urlShortenerUseCase.redirect(shortCode);

            urlPrinter.printSuccess("↪️ Перенаправление на: " +
                    truncate(originalUrl.value(), 60));

            if (enableAutoRedirect && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(new URI(originalUrl.value()));
                    urlPrinter.printWarning("🌐 Открываю в браузере...");
                } catch (Exception e) {
                    urlPrinter.printWarning("Не удалось открыть браузер: " + e.getMessage());
                    urlPrinter.printInfo("📋 URL скопируйте вручную: " + originalUrl.value());
                }
            } else {
                urlPrinter.printInfo("📋 URL: " + originalUrl.value());
            }

        } catch (Exception e) {
            urlPrinter.printError(e.getMessage());
        }
    }

    private void handleList() {
        var currentUser = userManagementUseCase.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        List<ShortenedUrl> urls = urlShortenerUseCase.getUserUrls(currentUser.getId());
        urlPrinter.printUserUrls(urls, baseUrl);
    }

    private void handleInfo(CommandParser.ParsedCommand parsed) {
        if (parsed.getArgCount() < 1) {
            urlPrinter.printError("Используйте: info <короткая_ссылка>");
            return;
        }

        try {
            ShortCode shortCode = ShortCode.fromShortUrl(parsed.getArg(0), baseUrl);
            ShortenedUrl url = urlShortenerUseCase.getUrlInfo(shortCode);
            urlPrinter.printUrlInfo(url, baseUrl, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            urlPrinter.printError(e.getMessage());
        }
    }

    private void handleEdit(CommandParser.ParsedCommand parsed) {
        if (parsed.getArgCount() < 1) {
            urlPrinter.printError("Используйте: edit <короткая_ссылка> [новый_url] [новая_дата]");
            return;
        }

        var currentUser = userManagementUseCase.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        try {
            ShortCode shortCode = ShortCode.fromShortUrl(parsed.getArg(0), baseUrl);
            Url newUrl = null;
            LocalDateTime newExpiresAt = null;

            if (parsed.getArgCount() >= 2) {
                String param = parsed.getArg(1);

                if (param.matches("^(http|https)://.+")) {
                    newUrl = new Url(param);

                    if (parsed.getArgCount() >= 3) {
                        try {
                            String dateTimeStr = parsed.getArg(2);
                            if (parsed.getArgCount() >= 4) {
                                dateTimeStr = parsed.getArg(2) + " " + parsed.getArg(3);
                            }
                            newExpiresAt = commandParser.parseDateTime(dateTimeStr);
                        } catch (IllegalArgumentException e) {
                            urlPrinter.printWarning("Неверный формат даты. Редактирую только URL");
                        }
                    }
                } else {
                    try {
                        String dateTimeStr = param;
                        if (parsed.getArgCount() >= 3) {
                            dateTimeStr = param + " " + parsed.getArg(2);
                        }
                        newExpiresAt = commandParser.parseDateTime(dateTimeStr);
                    } catch (IllegalArgumentException e) {
                        urlPrinter.printError("Неверный формат. Укажите URL или дату в формате ГГГГ-ММ-ДД ЧЧ:ММ");
                        return;
                    }
                }
            }

            ShortenedUrl updatedUrl = urlShortenerUseCase.editUrl(
                    shortCode, currentUser.getId(), newUrl, newExpiresAt);

            urlPrinter.printSuccess("Короткая ссылка успешно отредактирована");
            urlPrinter.printInfo("Новая ссылка: " + updatedUrl.getShortUrl(baseUrl));

        } catch (Exception e) {
            urlPrinter.printError(e.getMessage());
        }
    }

    private void handleDelete(CommandParser.ParsedCommand parsed) {
        if (parsed.getArgCount() < 1) {
            urlPrinter.printError("Используйте: delete <короткая_ссылка>");
            return;
        }

        var currentUser = userManagementUseCase.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        try {
            ShortCode shortCode = ShortCode.fromShortUrl(parsed.getArg(0), baseUrl);

            System.out.print(ConsoleColors.yellow("⚠️ Вы уверены, что хотите удалить короткую ссылку " +
                    parsed.getArg(0) + "? [y/N]: "));
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if ("y".equals(confirmation) || "yes".equals(confirmation)) {
                urlShortenerUseCase.deleteUrl(shortCode, currentUser.getId());
                urlPrinter.printSuccess("Ссылка успешно удалена");
            } else {
                urlPrinter.printInfo("Удаление отменено");
            }

        } catch (Exception e) {
            urlPrinter.printError(e.getMessage());
        }
    }

    private void handleSwitch(CommandParser.ParsedCommand parsed) {
        if (parsed.getArgCount() < 1) {
            urlPrinter.printError("Используйте: switch <uuid или короткий-id>");
            return;
        }

        try {
            // Используем новый метод сервиса
            if (userManagementUseCase instanceof UserServiceImpl) {
                ((UserServiceImpl) userManagementUseCase).switchUser(parsed.getArg(0));
                var currentUser = userManagementUseCase.getCurrentUser()
                        .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
                urlPrinter.printSuccess("Переключен на пользователя: " + currentUser.getShortId() + "...");
            } else {
                // Fallback для других реализаций
                UserId userId = UserId.fromString(parsed.getArg(0));
                userManagementUseCase.switchUser(userId);
                urlPrinter.printSuccess("Переключен на пользователя: " + userId.shortId() + "...");
            }
        } catch (IllegalArgumentException e) {
            urlPrinter.printError(e.getMessage());
        }
    }

    private void handleNewUser() {
        var user = userManagementUseCase.createUser();
        urlPrinter.printSuccess("🎉 Создан новый пользователь!");
        urlPrinter.printInfo("Ваш UUID: " + user.getId().toString());
        urlPrinter.printInfo("Короткий ID (для быстрого входа): " + user.getShortId());
        urlPrinter.printWarning("⚠️ Сохраните этот UUID для доступа к вашим ссылкам в будущем");
    }

    private void handleWhoAmI() {
        var currentUser = userManagementUseCase.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        System.out.println(ConsoleColors.bold("👤 Текущий пользователь: " + currentUser.getId()));
        System.out.println(ConsoleColors.yellow("📋 Короткий ID: " + currentUser.getShortId()));
        System.out.println(ConsoleColors.yellow("💡 Сохраните этот UUID для будущего доступа"));
    }

    private void handleStats() {
        var currentUser = userManagementUseCase.getCurrentUser();

        Map<String, Object> globalStats = statisticsUseCase.getGlobalStatistics();
        Map<String, Object> userStats = currentUser
                .map(user -> statisticsUseCase.getUserStatistics(user.getId()))
                .orElse(Map.of());

        urlPrinter.printStatistics(globalStats, userStats);
    }

    private void handleConfig() {
        Map<String, Object> configInfo = statisticsUseCase.getConfigInfo();
        urlPrinter.printConfig(configInfo);
    }

    private void shutdown() {
        urlPrinter.printWarning("👋 Завершение работы...");
        isRunning = false;
        scanner.close();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public void runNonInteractive(String command) {
        processCommand(command);
    }
}