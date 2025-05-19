package org.example;

import Dao.UserDAO;
import Dao.ScheduleDAO;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleBot extends TelegramLongPollingBot {

    private final String BOT_USERNAME = "ScheduleComBot";
    private final String BOT_TOKEN = "7739118437:AAFaeMz-85h3X324yN6PR-br7MxKNMc4HQY";

    private UserDAO userDAO = new UserDAO();
    private ScheduleDAO scheduleDAO = new ScheduleDAO();

    // Словарь для хранения состояний залогиненных пользователей: chatId -> idCard
    private Map<Long, String> loggedInUsers = new HashMap<>();

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    private void sendMessage(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboardMarkup != null) {
            message.setReplyMarkup(keyboardMarkup);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Главное меню с кнопками Вход и Регистрация
    private void sendMainMenu(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Вход"));
        row.add(new KeyboardButton("Регистрация"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        sendMessage(chatId, "Выберите действие:", keyboardMarkup);
    }

    // Меню с кнопкой Назад и произвольным текстом
    private void sendBackMenu(long chatId, String text) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Назад"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        sendMessage(chatId, text, keyboardMarkup);
    }

    // Меню с кнопками дней недели + кнопка Стоп
    private void sendWeekMenu(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Понедельник"));
        row1.add(new KeyboardButton("Вторник"));
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Среда"));
        row2.add(new KeyboardButton("Четверг"));
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Пятница"));
        row3.add(new KeyboardButton("Суббота"));
        keyboard.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("Стоп"));
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        sendMessage(chatId, "Выберите день недели:", keyboardMarkup);
    }

    // Клавиатура с одной кнопкой Назад
    private ReplyKeyboardMarkup createBackKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Назад"));

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    // Обработка ввода логина/пароля или регистрации
    private void handleInput(long chatId, String text) {
        String[] parts = text.split("\\s+");
        if (parts.length == 2) {
            // Вход
            String idCard = parts[0];
            String password = parts[1];
            if (userDAO.userExists(idCard, password)) {
                loggedInUsers.put(chatId, idCard);
                sendMessage(chatId, "Вход успешен! Добро пожаловать.", null);
                sendWeekMenu(chatId);
            } else {
                sendMessage(chatId, "Неверный ID Card или пароль. Попробуйте снова.", null);
            }
        } else if (parts.length == 3) {
            // Регистрация
            String idCard = parts[0];
            String password = parts[1];
            String direction = parts[2];
            boolean success = userDAO.registerUser(idCard, password, direction);
            if (success) {
                sendMessage(chatId, "Регистрация прошла успешно! Можете войти.", null);
            } else {
                sendMessage(chatId, "Ошибка регистрации: возможно, пользователь с таким ID Card уже существует.", null);
            }
        } else {
            sendMessage(chatId, "Неверный формат ввода. Пожалуйста, используйте правильный формат.", null);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();

            switch (text) {
                case "/start" -> sendMainMenu(chatId);

                case "Вход" -> sendBackMenu(chatId, "Введите ваш ID Card и пароль через пробел, например:\n12345 password123");

                case "Регистрация" -> sendBackMenu(chatId, "Введите ID Card, пароль и направление через пробел, например:\n12345 password123 IT");

                case "Назад" -> {
                    if (loggedInUsers.containsKey(chatId)) {
                        sendWeekMenu(chatId);
                    } else {
                        sendMainMenu(chatId);
                    }
                }

                case "Стоп" -> {
                    if (loggedInUsers.containsKey(chatId)) {
                        loggedInUsers.remove(chatId);
                        sendMessage(chatId, "Сессия завершена. Для начала работы снова используйте /start", null);
                        sendMainMenu(chatId);
                    } else {
                        sendMessage(chatId, "Вы не вошли в систему.", null);
                        sendMainMenu(chatId);
                    }
                }

                case "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота" -> {
                    if (loggedInUsers.containsKey(chatId)) {
                        String schedule = scheduleDAO.getScheduleByDay(text);
                        sendMessage(chatId, schedule, createBackKeyboard());
                    } else {
                        sendMessage(chatId, "Пожалуйста, войдите в систему, чтобы просмотреть расписание.", null);
                    }
                }

                default -> handleInput(chatId, text);
            }
        }
    }
}
