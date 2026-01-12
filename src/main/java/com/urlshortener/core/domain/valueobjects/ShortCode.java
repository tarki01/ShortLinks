package com.urlshortener.core.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Value Object для короткого кода ссылки
 */
public final class ShortCode {
    private final String value;

    @JsonCreator
    public ShortCode(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Короткий код не может быть пустым");
        }
        if (value.length() < 3 || value.length() > 10) {
            throw new IllegalArgumentException("Короткий код должен содержать 3-10 символов");
        }
        if (!value.matches("^[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException("Короткий код должен содержать только латинские буквы а также цифры");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static ShortCode fromShortUrl(String shortUrl, String baseUrl) {
        if (!shortUrl.startsWith(baseUrl)) {
            throw new IllegalArgumentException("Короткий URL должен стартовать с Base URL");
        }

        String code = shortUrl.substring(baseUrl.length());
        if (code.contains("/")) {
            code = code.substring(0, code.indexOf("/"));
        }
        if (code.contains("?")) {
            code = code.substring(0, code.indexOf("?"));
        }

        return new ShortCode(code);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShortCode shortCode = (ShortCode) o;
        return Objects.equals(value, shortCode.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}