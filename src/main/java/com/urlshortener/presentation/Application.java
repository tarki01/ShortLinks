package com.urlshortener.presentation;
import com.urlshortener.presentation.cli.StartingUp;
/**
 * Точка входа в приложение
 */
public class Application {

    public static void main(String[] args) {
        StartingUp startingUp = new StartingUp();
        startingUp.start();
    }
}