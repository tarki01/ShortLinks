package com.urlshortener.core.domain.models;

import com.urlshortener.core.domain.valueobjects.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Доменная модель пользователя
 */
public class User {
    private final UserId id;
    private final LocalDateTime createdAt;
    private final Set<String> shortCodes; // Коды ссылок пользователя

    public User(UserId id) {
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.createdAt = LocalDateTime.now();
        this.shortCodes = new HashSet<>();
    }

    @JsonIgnore
    public int getUrlCount() {
        return shortCodes.size();
    }

    // Getters
    public UserId getId() {
        return id;
    }

    @JsonIgnore
    public String getShortId() {
        return id.shortId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("User{id=%s, urlCount=%d}", id.shortId(), getUrlCount());
    }
}