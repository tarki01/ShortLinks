package com.urlshortener.infrastructure.utils;

import com.urlshortener.core.ports.output.UrlValidator;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Реализация валидатора URL
 */
public class UrlValidatorImpl implements UrlValidator {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^((http|https)://)[a-zA-Z0-9@:%._\\+~#?&//=]" +
                    "{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)$"
    );

    private static final String[] ALLOWED_SCHEMES = {"http", "https", "ftp"};

    @Override
    public boolean isValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        if (url.length() > 2048) {
            return false;
        }

        // Проверка регулярным выражением
        if (!URL_PATTERN.matcher(url).matches()) {
            String urlWithScheme = "https://" + url;
            if (!URL_PATTERN.matcher(urlWithScheme).matches()) {
                return false;
            }
        }

        try {
            URL parsedUrl = new URL(normalize(url));

            // Проверка схемы
            String scheme = parsedUrl.getProtocol();
            boolean validScheme = false;
            for (String allowedScheme : ALLOWED_SCHEMES) {
                if (allowedScheme.equalsIgnoreCase(scheme)) {
                    validScheme = true;
                    break;
                }
            }
            if (!validScheme) {
                return false;
            }

            // Проверка хоста
            String host = parsedUrl.getHost();
            if (host == null || host.trim().isEmpty()) {
                return false;
            }

            // Дополнительная валидация через URI
            parsedUrl.toURI();

            return true;

        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String normalize(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }

        url = url.trim();
        url = url.replaceAll("[\\p{Cntrl}\\s]+", "");

        if (url.matches("^(http|https|ftp)://.*")) {
            return url;
        }

        return "https://" + url;
    }

    @Override
    public boolean isSecure(String url) {
        if (!isValid(url)) {
            return false;
        }

        try {
            URL parsedUrl = new URL(normalize(url));
            return "https".equalsIgnoreCase(parsedUrl.getProtocol());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public boolean isLocal(String url) {
        if (!isValid(url)) {
            return false;
        }

        try {
            URL parsedUrl = new URL(normalize(url));
            String host = parsedUrl.getHost().toLowerCase();

            return host.equals("localhost") ||
                    host.equals("127.0.0.1") ||
                    host.equals("::1") ||
                    host.equals("0.0.0.0") ||
                    host.startsWith("192.168.") ||
                    host.startsWith("10.") ||
                    host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*") ||
                    host.endsWith(".local") ||
                    host.endsWith(".internal");
        } catch (MalformedURLException e) {
            return false;
        }
    }
}