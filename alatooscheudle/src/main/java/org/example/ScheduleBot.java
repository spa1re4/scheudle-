package org.example;

import Dao.ScheduleDAO;
import Dao.UserDAO;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class ScheduleBot extends TelegramLongPollingBot {

    private final String BOT_USERNAME = "ScheduleComBot";
    private final String BOT_TOKEN = "7739118437:AAFaeMz-85h3X324yN6PR-br7MxKNMc4HQY";

    private final UserDAO userDAO = new UserDAO();
    private final ScheduleDAO scheduleDAO = new ScheduleDAO();

    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, String> userDirection = new HashMap<>();

    private final List<String> directions = Arrays.asList("comfci-23", "comceh-23", "comse-23", "eeair-23", "iemit-23");
    private final List<String> daysOfWeek = Arrays.asList("Понедельник", "Вторник", "Среда", "Четверг", "Пятница");

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

    private void sendStartMenu(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Вход"));
        row.add(new KeyboardButton("Регистрация"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);

        sendMessage(chatId, "Добро пожаловать! Выберите действие:", keyboardMarkup);
        userState.put(chatId, "start");
    }

    private void sendDirectionMenu(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String dir : directions) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(dir));
            keyboard.add(row);
        }

        KeyboardRow stopRow = new KeyboardRow();
        stopRow.add(new KeyboardButton("Назад"));
        stopRow.add(new KeyboardButton("Стоп"));
        keyboard.add(stopRow);

        keyboardMarkup.setKeyboard(keyboard);

        sendMessage(chatId, "Выберите направление:", keyboardMarkup);
        userState.put(chatId, "selecting_direction");
    }

    private void sendDayMenu(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String day : daysOfWeek) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(day));
            keyboard.add(row);
        }

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Назад"));
        row.add(new KeyboardButton("Стоп"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);

        sendMessage(chatId, "Выберите день недели:", keyboardMarkup);
        userState.put(chatId, "selecting_day");
    }

    private void sendSchedule(long chatId, String direction, String day) {
        String schedule = scheduleDAO.getSchedule(direction, day);
        sendMessage(chatId, "Расписание для " + direction + " на " + day + ":\n" + schedule, null);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();

            switch (text) {
                case "/start":
                    userState.remove(chatId);
                    userDirection.remove(chatId);
                    sendStartMenu(chatId);
                    return;

                case "Вход":
                case "Регистрация":
                    sendMessage(chatId, "Введите ID Card и пароль через пробел:", null);
                    userState.put(chatId, text.equals("Вход") ? "awaiting_login" : "awaiting_register");
                    return;

                case "Назад":
                    String state = userState.getOrDefault(chatId, "");
                    if (state.equals("selecting_day")) {
                        sendDirectionMenu(chatId);
                    } else {
                        sendStartMenu(chatId);
                    }
                    return;

                case "Стоп":
                    userState.remove(chatId);
                    userDirection.remove(chatId);
                    sendStartMenu(chatId);
                    return;
            }

            String state = userState.getOrDefault(chatId, "");

            switch (state) {
                case "awaiting_login": {
                    String[] parts = text.split("\\s+");
                    if (parts.length == 2) {
                        boolean exists = userDAO.userExists(parts[0], parts[1]);
                        if (exists) {
                            sendDirectionMenu(chatId);
                        } else {
                            sendMessage(chatId, "Неверный ID Card или пароль. Попробуйте снова.", null);
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат. Введите ID и пароль через пробел.", null);
                    }
                    return;
                }

                case "awaiting_register": {
                    String[] parts = text.split("\\s+");
                    if (parts.length == 2) {
                        boolean success = userDAO.registerUser(parts[0], parts[1]);
                        String response = success
                                ? "Регистрация прошла успешно! Теперь войдите."
                                : "Ошибка регистрации. Возможно, ID уже зарегистрирован.";
                        sendMessage(chatId, response, null);
                    } else {
                        sendMessage(chatId, "Неверный формат. Введите ID и пароль через пробел.", null);
                    }
                    return;
                }

                case "selecting_direction":
                    if (directions.contains(text)) {
                        userDirection.put(chatId, text);
                        sendDayMenu(chatId);
                    } else {
                        sendMessage(chatId, "Выберите направление из списка.", null);
                    }
                    return;

                case "selecting_day":
                    if (daysOfWeek.contains(text)) {
                        String direction = userDirection.get(chatId);
                        if (direction != null) {
                            sendSchedule(chatId, direction, text);
                        } else {
                            sendMessage(chatId, "Ошибка: направление не выбрано.", null);
                        }
                    } else {
                        sendMessage(chatId, "Выберите день недели из списка.", null);
                    }
                    return;

                default:
                    sendMessage(chatId, "Неизвестная команда. Нажмите /start.", null);
            }
        }
    }
}
