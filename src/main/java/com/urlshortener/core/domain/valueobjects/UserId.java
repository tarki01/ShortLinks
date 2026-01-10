package com.urlshortener.core.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object для идентификатора пользователя
 */
public final class UserId {
    private final UUID value;

    @JsonCreator
    public UserId(UUID value) {
        this.value = Objects.requireNonNull(value, "User ID cannot be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    @JsonCreator
    public static UserId fromString(String uuid) {
        try {
            return new UserId(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuid);
        }
    }

    @JsonValue
    public UUID value() {
        return value;
    }

    public String shortId() {
        return value.toString().substring(0, 8);
    }

    public boolean matchesShortId(String shortId) {
        return shortId != null &&
                shortId.length() >= 8 &&
                value.toString().toLowerCase().startsWith(shortId.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}