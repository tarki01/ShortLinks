package com.urlshortener.infrastructure.persistence;

import com.urlshortener.core.domain.models.User;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.ports.output.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация репозитория пользователей в памяти
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> storage; // userId -> User
    private final Map<String, UUID> shortIdToFullId; // shortId -> full UUID

    public InMemoryUserRepository() {
        this.storage = new HashMap<>();
        this.shortIdToFullId = new HashMap<>();
    }

    @Override
    public User save(User user) {
        storage.put(user.getId().value(), user);
        shortIdToFullId.put(user.getShortId(), user.getId().value());
        return user;
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(storage.get(userId.value()));
    }

    @Override
    public Optional<User> findByShortId(String shortId) {
        if (shortId == null || shortId.length() < 8) {
            return Optional.empty();
        }

        // Ищем полный UUID по короткому ID
        UUID fullId = shortIdToFullId.get(shortId.toLowerCase());
        if (fullId != null) {
            return Optional.ofNullable(storage.get(fullId));
        }

        // Если не нашли в индексе, ищем по всем пользователям
        return storage.values().stream()
                .filter(user -> user.getId().matchesShortId(shortId))
                .findFirst();
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public Iterable<User> findAll() {
        return storage.values();
    }
}