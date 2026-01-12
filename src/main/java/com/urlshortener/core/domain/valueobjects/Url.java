package com.urlshortener.core.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Value Object для URL
 */
public final class Url {
    private final String value;

    @JsonCreator
    public Url(String value) {
        validate(value);
        this.value = normalize(value);
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }
        if (value.length() > 2048) {
            throw new IllegalArgumentException("URL слишком длинное");
        }
    }

    private String normalize(String url) {
        url = url.trim();

        // Удаление лишних пробелов и управляющих символов
        url = url.replaceAll("[\\p{Cntrl}\\s]+", "");

        // Добавление протокола если нет
        if (!url.matches("^(http|https|ftp)://.*")) {
            return "https://" + url;
        }

        return url;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Url url = (Url) o;
        return Objects.equals(value, url.value);
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