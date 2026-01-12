package com.urlshortener.presentation.cli;

import com.urlshortener.core.domain.models.ShortenedUrl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Принтер для вывода информации о ссылках
 */
public class UrlPrinter {

    public void printBanner() {
        System.out.println();
        System.out.println(ConsoleColors.cyan("╔══════════════════════════════════════════════════════════════════════╗"));
        System.out.println(ConsoleColors.bold(ConsoleColors.purple(
                "                      🔗 СЕРВИС СОКРАЩЕНИЯ ССЫЛОК                ")));
        System.out.println(ConsoleColors.cyan("╚══════════════════════════════════════════════════════════════════════╝"));
        System.out.println();
    }

    public void printHelp() {
        System.out.println(ConsoleColors.bold(ConsoleColors.blue("📖 ДОСТУПНЫЕ КОМАНДЫ:")));
        System.out.println("┌─────────────────────────────────────────────────────────────────────-");
        System.out.println("│ " + ConsoleColors.green("sh <url> [дата] [переходы]") + " - Сократить URL с параметами ");
        System.out.println("│ " + ConsoleColors.green("go <короткая_ссылка>") + "         - Перейти по короткой ссылке    ");
        System.out.println("│ " + ConsoleColors.green("list") + "                       - Мои ссылки                   ");
        System.out.println("│ " + ConsoleColors.green("info <короткая_ссылка>") + "       - Информация о ссылке          ");
        System.out.println("│ " + ConsoleColors.green("edit <короткая_ссылка> <url> <дата>") + " - Редактировать ссылку  ");
        System.out.println("│ " + ConsoleColors.green("delete <короткая_ссылка>") + "     - Удалить мою ссылку           ");
        System.out.println("│ " + ConsoleColors.green("switch <user_id>") + "           - Сменить пользователя         ");
        System.out.println("│ " + ConsoleColors.green("newuser") + "                    - Создать нового пользователя  ");
        System.out.println("│ " + ConsoleColors.green("whoami") + "                     - Текущий пользователь         ");
        System.out.println("│ " + ConsoleColors.green("stats") + "                      - Статистика                   ");
        System.out.println("│ " + ConsoleColors.green("config") + "                     - Показать конфигурацию        ");
        System.out.println("│ " + ConsoleColors.green("help") + "                       - Эта справка                  ");
        System.out.println("│ " + ConsoleColors.green("exit") + "                       - Выйти из программы           ");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println(ConsoleColors.bold(ConsoleColors.yellow("💡 ПРИМЕРЫ КОМАНДЫ SHORTEN:")));
        System.out.println("  " + ConsoleColors.cyan("sh https://google.com") + " - сократить с параметрами по умолчанию");
        System.out.println("  " + ConsoleColors.cyan("sh https://google.com 2026-12-31 23:59") + " - с датой истечения");
        System.out.println();
    }

    public void printUrlInfo(ShortenedUrl url, String baseUrl, DateTimeFormatter formatter) {
        System.out.println();
        System.out.println(ConsoleColors.bold(ConsoleColors.blue("📊 ИНФОРМАЦИЯ О ССЫЛКЕ")));
        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.println("│ " + ConsoleColors.cyan("Короткая ссылка: ") + url.getShortUrl(baseUrl));
        System.out.println("│ " + ConsoleColors.cyan("Оригинальный URL: ") +
                truncate(url.getOriginalUrl().value(), 50));
        System.out.println("│ " + ConsoleColors.cyan("Создана: ") + url.getCreatedAt().format(formatter));
        System.out.println("│ " + ConsoleColors.cyan("Истекает: ") + url.getExpiresAt().format(formatter) +
                " (" + url.getRemainingHours() + "ч осталось)");

        String status;
        if (url.canBeAccessed()) {
            status = ConsoleColors.green("Активна");
        } else if (url.isExpired()) {
            status = ConsoleColors.red("Истекла");
        } else {
            status = ConsoleColors.yellow("Неактивна");
        }

        System.out.println("│ " + ConsoleColors.cyan("Статус: ") + status);
        System.out.println("│ " + ConsoleColors.cyan("Переходы: ") +
                url.getCurrentClicks() + "/" + url.getMaxClicks() +
                " (" + url.getRemainingClicks() + " осталось)");
        System.out.println("│ " + ConsoleColors.cyan("Владелец: ") +
                url.getUserId().shortId() + "...");
        System.out.println("└────────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    public void printUserUrls(List<ShortenedUrl> urls, String baseUrl) {
        if (urls.isEmpty()) {
            System.out.println(ConsoleColors.yellow("📭 У вас пока нет сокращенных ссылок"));
            System.out.println("   Используйте команду " +
                    ConsoleColors.green("sh <url>") + " чтобы создать первую");
            return;
        }

        System.out.println();
        System.out.println(ConsoleColors.bold(ConsoleColors.blue("📋 ВАШИ ССЫЛКИ (" + urls.size() + ")")));
        System.out.println("┌──────────────────────────────────┬──────────────────────────────────────────┬──────────┬────────────┬────────────────────┐");
        System.out.println("│          Короткая ссылка         │                 URL                      │ Переходы │   Статус   │     Истекает      │");
        System.out.println("├──────────────────────────────────┼──────────────────────────────────────────┼──────────┼────────────┼────────────────────┤");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

        for (ShortenedUrl url : urls) {
            String shortUrl = url.getShortUrl(baseUrl);
            String displayShortUrl = truncate(shortUrl, 30);

            String displayUrl = truncate(url.getOriginalUrl().value(), 40);
            String expiresAt = url.getExpiresAt().format(formatter);

            System.out.printf("│ %-30s │ %-40s │ %6d/%d │ %-12s │ %-16s │\n",
                    displayShortUrl,
                    displayUrl,
                    url.getCurrentClicks(),
                    url.getMaxClicks(),
                    url.getStatus(),
                    expiresAt
            );
        }

        System.out.println("└──────────────────────────────────┴──────────────────────────────────────────┴──────────┴────────────┴────────────────────┘");

        // Статистика
        long activeCount = urls.stream().filter(ShortenedUrl::canBeAccessed).count();
        int totalClicks = urls.stream().mapToInt(ShortenedUrl::getCurrentClicks).sum();

        System.out.println();
        System.out.println(ConsoleColors.bold("📊 СТАТИСТИКА: ") +
                activeCount + " активных, " +
                totalClicks + " всего переходов");
    }

    public void printStatistics(Map<String, Object> globalStats, Map<String, Object> userStats) {
        System.out.println();
        System.out.println(ConsoleColors.bold(ConsoleColors.blue("📊 ГЛОБАЛЬНАЯ СТАТИСТИКА")));
        System.out.println("┌────────────────────────────────────────────────────────────┐");
        printStatRow("Всего ссылок", globalStats.get("totalUrls"));
        printStatRow("Всего пользователей", globalStats.get("totalUsers"));
        printStatRow("Активных ссылок", globalStats.get("activeUrls"));
        printStatRow("Просроченных ссылок", globalStats.get("expiredUrls"));
        System.out.println("└────────────────────────────────────────────────────────────┘");

        if (userStats != null && !userStats.isEmpty()) {
            System.out.println();
            System.out.println(ConsoleColors.bold(ConsoleColors.green("👤 ВАША СТАТИСТИКА")));
            System.out.println("┌────────────────────────────────────────────────────────────┐");
            printStatRow("Ваших ссылок", userStats.get("totalUrls"));
            printStatRow("Ваших переходов", userStats.get("totalClicks"));
            printStatRow("Активных ссылок", userStats.get("activeUrls"));
            System.out.println("└────────────────────────────────────────────────────────────┘");
        }
        System.out.println();
    }

    public void printConfig(Map<String, Object> configInfo) {
        System.out.println();
        System.out.println(ConsoleColors.bold(ConsoleColors.blue("⚙️ КОНФИГУРАЦИЯ ПРИЛОЖЕНИЯ")));
        System.out.println("┌────────────────────────────────────────────────────────────┐");
        printConfigRow("Базовый URL", configInfo.get("baseUrl"));
        printConfigRow("TTL по умолчанию (часы)", configInfo.get("defaultTTLHours"));
        printConfigRow("Макс. переходов", configInfo.get("defaultMaxClicks"));
        printConfigRow("Длина кода", configInfo.get("shortCodeLength"));
        printConfigRow("Файл данных", configInfo.get("storageFile"));
        printConfigRow("Очистка (мин)", configInfo.get("cleanupIntervalMinutes"));
        printConfigRow("Макс. срок (дни)", configInfo.get("maxTTLDays"));
        printConfigRow("Формат даты", configInfo.get("dateTimeFormat"));
        printConfigRow("Авто-редирект",
                Boolean.TRUE.equals(configInfo.get("enableAutoRedirect")) ? "Да" : "Нет");
        System.out.println("└────────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    public void printSuccess(String message) {
        System.out.println(ConsoleColors.green("✅ " + message));
    }

    public void printError(String message) {
        System.out.println(ConsoleColors.red("❌ " + message));
    }

    public void printWarning(String message) {
        System.out.println(ConsoleColors.yellow("⚠️ " + message));
    }

    public void printInfo(String message) {
        System.out.println(ConsoleColors.cyan("ℹ️ " + message));
    }

    private void printStatRow(String label, Object value) {
        System.out.printf("│ %-30s: %-25s │\n", label, value);
    }

    private void printConfigRow(String label, Object value) {
        System.out.printf("│ %-20s: %-35s │\n", label, value);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}