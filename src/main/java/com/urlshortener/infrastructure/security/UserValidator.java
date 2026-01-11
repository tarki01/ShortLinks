package com.urlshortener.infrastructure.security;

import java.util.regex.Pattern;

/**
 * Валидатор пользователей
 */
public class UserValidator {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
    );

    public static boolean isValidUuid(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid).matches();
    }

    public static boolean isValidShortId(String shortId) {
        return shortId != null &&
                shortId.length() >= 8 &&
                shortId.matches("^[0-9a-f]+$");
    }
}