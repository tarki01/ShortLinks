package com.urlshortener.presentation.cli;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Парсер команд CLI
 */
public class CommandParser {

    public enum CommandType {
        SHORTEN, GO, LIST, INFO, EDIT, DELETE,
        SWITCH, NEWUSER, WHOAMI, STATS,
        CONFIG, HELP, EXIT, UNKNOWN
    }

    public static class ParsedCommand {
        private final CommandType type;
        private final List<String> args;

        public ParsedCommand(CommandType type, List<String> args) {
            this.type = type;
            this.args = args;
        }

        public CommandType getType() {
            return type;
        }

        public String getArg(int index) {
            return index < args.size() ? args.get(index) : null;
        }

        public int getArgCount() {
            return args.size();
        }
    }

    public ParsedCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ParsedCommand(CommandType.UNKNOWN, List.of());
        }

        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) {
            return new ParsedCommand(CommandType.UNKNOWN, List.of());
        }

        String commandStr = parts[0].toLowerCase();
        List<String> args = Arrays.asList(parts).subList(1, parts.length);

        CommandType type = parseCommandType(commandStr);
        return new ParsedCommand(type, args);
    }

    private CommandType parseCommandType(String command) {
        return switch (command) {
            case "shorten" -> CommandType.SHORTEN;
            case "go" -> CommandType.GO;
            case "list" -> CommandType.LIST;
            case "info" -> CommandType.INFO;
            case "edit" -> CommandType.EDIT;
            case "delete" -> CommandType.DELETE;
            case "switch" -> CommandType.SWITCH;
            case "newuser" -> CommandType.NEWUSER;
            case "whoami" -> CommandType.WHOAMI;
            case "stats" -> CommandType.STATS;
            case "config" -> CommandType.CONFIG;
            case "help" -> CommandType.HELP;
            case "exit" -> CommandType.EXIT;
            default -> CommandType.UNKNOWN;
        };
    }

    public LocalDateTime parseDateTime(String dateTimeStr) throws DateTimeParseException {
        try {
            // Пробуем разные форматы
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("HH:mm"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    DateTimeFormatter.ISO_LOCAL_DATE
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    if (dateTimeStr.length() == 10) { // yyyy-MM-dd
                        return LocalDateTime.parse(dateTimeStr + " 23:59",
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    }
                    return LocalDateTime.parse(dateTimeStr, formatter);
                } catch (DateTimeParseException e) {
                    // Пробуем следующий формат
                }
            }

            throw new DateTimeParseException("Неверный формат даты", dateTimeStr, 0);

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("❌ Неверный формат даты. Используйте: ГГГГ-ММ-ДД ЧЧ:ММ");
        }
    }

    public Integer parseInteger(String intStr, String fieldName) {
        try {
            int value = Integer.parseInt(intStr);
            if (value <= 0) {
                throw new IllegalArgumentException(
                        "❌ " + fieldName + " должно быть положительным числом"
                );
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "❌ Неверный формат " + fieldName + ". Используйте целое число"
            );
        }
    }
}