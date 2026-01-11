package com.urlshortener.infrastructure.utils;

import com.urlshortener.core.domain.valueobjects.ShortCode;
import com.urlshortener.core.domain.valueobjects.Url;
import com.urlshortener.core.domain.valueobjects.UserId;
import com.urlshortener.core.ports.output.IdGenerator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Реализация генератора коротких кодов
 */
public class CodeGeneratorImpl implements IdGenerator {

    private static final String BASE62_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String READABLE_ALPHABET =
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";

    private static final Map<Character, Character> SIMILAR_CHARS = new HashMap<>();

    static {
        SIMILAR_CHARS.put('0', 'O');
        SIMILAR_CHARS.put('O', '0');
        SIMILAR_CHARS.put('1', 'I');
        SIMILAR_CHARS.put('I', '1');
        SIMILAR_CHARS.put('l', '1');
        SIMILAR_CHARS.put('5', 'S');
        SIMILAR_CHARS.put('S', '5');
        SIMILAR_CHARS.put('8', 'B');
        SIMILAR_CHARS.put('B', '8');
    }

    @Override
    public ShortCode generate(Url originalUrl, UserId userId) {
        return generate(originalUrl, userId, 6);
    }

    @Override
    public ShortCode generate(Url originalUrl, UserId userId, int length) {
        validateLength(length);

        String uniqueString = originalUrl.value() + userId.toString() + System.currentTimeMillis();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(uniqueString.getBytes());

            String code = bytesToBase62(hashBytes, length);
            code = makeReadable(code);

            // Гарантируем нужную длину
            if (code.length() > length) {
                code = code.substring(0, length);
            } else if (code.length() < length) {
                code = padCode(code, length);
            }

            return new ShortCode(code);

        } catch (NoSuchAlgorithmException e) {
            // Fallback
            return fallbackGenerate(originalUrl, userId, length);
        }
    }

    @Override
    public ShortCode generateForUser(Url originalUrl, UserId userId, int length,
                                     Map<String, String> existingCodes) {
        ShortCode code = generate(originalUrl, userId, length);
        int attempts = 0;
        int maxAttempts = 10;

        // Проверяем уникальность для пользователя
        while (existingCodes.containsKey(code.value()) && attempts < maxAttempts) {
            String existingUserId = existingCodes.get(code.value());
            if (existingUserId.equals(userId.toString())) {
                // Добавляем суффикс
                code = addSuffix(code, attempts, length);
            } else {
                break;
            }
            attempts++;
        }

        return code;
    }

    @Override
    public boolean isValid(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        if (code.length() < 3 || code.length() > 10) {
            return false;
        }

        for (char c : code.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }

        return true;
    }

    // Приватные вспомогательные методы
    private void validateLength(int length) {
        if (length < 4 || length > 10) {
            throw new IllegalArgumentException("Длина кода должна быть от 4 до 10 символов");
        }
    }

    private String bytesToBase62(byte[] bytes, int length) {
        StringBuilder result = new StringBuilder();
        long number = 0;

        for (int i = 0; i < Math.min(8, bytes.length); i++) {
            number = (number << 8) | (bytes[i] & 0xFF);
        }

        while (number > 0 && result.length() < length) {
            int remainder = (int) (number % BASE62_ALPHABET.length());
            result.append(BASE62_ALPHABET.charAt(remainder));
            number = number / BASE62_ALPHABET.length();
        }

        while (result.length() < length) {
            result.append(BASE62_ALPHABET.charAt(
                    Math.abs(bytes[result.length() % bytes.length]) % BASE62_ALPHABET.length()
            ));
        }

        return result.reverse().toString();
    }

    private String makeReadable(String code) {
        char[] chars = code.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (SIMILAR_CHARS.containsKey(c)) {
                int index = Math.abs(c) % READABLE_ALPHABET.length();
                chars[i] = READABLE_ALPHABET.charAt(index);
            }
        }

        return new String(chars);
    }

    private ShortCode addSuffix(ShortCode code, int attempt, int maxLength) {
        String codeStr = code.value();

        if (codeStr.length() < maxLength) {
            int suffixIndex = attempt % READABLE_ALPHABET.length();
            return new ShortCode(codeStr + READABLE_ALPHABET.charAt(suffixIndex));
        } else {
            char[] chars = codeStr.toCharArray();
            int newIndex = (READABLE_ALPHABET.indexOf(chars[chars.length - 1]) + attempt + 1)
                    % READABLE_ALPHABET.length();
            chars[chars.length - 1] = READABLE_ALPHABET.charAt(newIndex);
            return new ShortCode(new String(chars));
        }
    }

    private String padCode(String code, int length) {
        StringBuilder padded = new StringBuilder(code);
        while (padded.length() < length) {
            int index = Math.abs(padded.toString().hashCode()) % READABLE_ALPHABET.length();
            padded.append(READABLE_ALPHABET.charAt(index));
        }
        return padded.toString();
    }

    private ShortCode fallbackGenerate(Url originalUrl, UserId userId, int length) {
        String uniqueString = originalUrl.value() + userId.toString() + System.nanoTime();
        int hash = Math.abs(uniqueString.hashCode());

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (hash + i * 31) % READABLE_ALPHABET.length();
            code.append(READABLE_ALPHABET.charAt(index));
        }

        return new ShortCode(code.toString());
    }
}