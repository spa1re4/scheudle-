package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class MyBot extends TelegramLongPollingBot {
    private static final Set<Long> ADMIN_IDS = Set.of(
            1237259277L,
            778224810L
    );

    private Map<Long, String> userStates = new HashMap<>();
    private Map<Long, String> tempPasswords = new HashMap<>();
    private Map<Long, String> tempGroups = new HashMap<>();

    @Override
    public String getBotUsername() {
        return "ScheduleComBot";
    }

    @Override
    public String getBotToken() {
        return "7739118437:AAFaeMz-85h3X324yN6PR-br7MxKNMc4HQY";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        Message msg = update.getMessage();
        long chatId = msg.getChatId();
        String text = msg.getText().trim();
        String state = userStates.getOrDefault(chatId, "");

        System.out.println("📩 Получено сообщение от " + chatId + ": " + text + " [state: " + state + "]");

        if (text.equals("⛔ Стоп")) {
            userStates.put(chatId, "");
            sendText(chatId, "❌ Сеанс завершён.");
            sendMainMenu(chatId);
            return;
        }

        if (text.equals("/start")) {
            sendMainMenu(chatId);
            userStates.put(chatId, "");
        }

        else if (text.equals("/update_schedule") && ADMIN_IDS.contains(chatId)) {
            sendText(chatId, "🗓 Введите новое расписание в формате: группа-день:расписание\nПример: comfci-23-Понедельник:08:00 Математика");
            userStates.put(chatId, "WAITING_SCHEDULE_UPDATE");
        }

        else if (state.equals("WAITING_SCHEDULE_UPDATE") && ADMIN_IDS.contains(chatId)) {
            String[] parts = text.split(":", 2);
            if (parts.length != 2) {
                sendText(chatId, "⚠️ Неверный формат. Пример: comfci-23-Понедельник:08:00 Математика");
                userStates.put(chatId, "");
                return;
            }

            String groupAndDay = parts[0].trim();
            String scheduleText = parts[1].trim();

            int lastDashIndex = groupAndDay.lastIndexOf("-");
            if (lastDashIndex == -1) {
                sendText(chatId, "⚠️ Неверный формат. Пример: comfci-23-Понедельник:08:00 Математика");
                userStates.put(chatId, "");
                return;
            }

            String group = groupAndDay.substring(0, lastDashIndex).trim();
            String day = groupAndDay.substring(lastDashIndex + 1).trim();

            System.out.println("➡️ Обновление расписания: " + group + " " + day + " -> " + scheduleText);

            boolean success = ScheduleDao.updateSchedule(group, day, scheduleText, this);
            if (success) {
                sendText(chatId, "✅ Расписание обновлено для *" + group + "*, день *" + day + "*.");
            } else {
                sendText(chatId, "❌ Ошибка при обновлении. Проверьте данные.");
            }

            userStates.put(chatId, "");
        }

        else if (text.equals("/send_message") && ADMIN_IDS.contains(chatId)) {
            sendText(chatId, "✉️ Введите текст сообщения для всех студентов:");
            userStates.put(chatId, "WAITING_BROADCAST_TEXT");
        }

        else if (state.equals("WAITING_BROADCAST_TEXT") && ADMIN_IDS.contains(chatId)) {
            List<Long> allUsers = UserDao.getAllUserChatIds();
            for (Long userId : allUsers) {
                sendText(userId, "📢 Сообщение от администрации:\n" + text);
            }
            sendText(chatId, "✅ Сообщение отправлено всем студентам.");
            userStates.put(chatId, "");
        }

        else if (text.equals("Регистрация")) {
            sendText(chatId, "Введите пароль:");
            userStates.put(chatId, "REG_PASSWORD");
        }

        else if (state.equals("REG_PASSWORD")) {
            tempPasswords.put(chatId, text);
            sendText(chatId, "Введите группу (например, comfci-23):");
            userStates.put(chatId, "REG_GROUP");
        }

        else if (state.equals("REG_GROUP")) {
            tempGroups.put(chatId, text);
            boolean success = UserDao.registerUser(chatId, tempPasswords.get(chatId), text);
            if (success) {
                sendText(chatId, "✅ Регистрация прошла успешно!");
            } else {
                sendText(chatId, "⚠️ Ошибка регистрации. Возможно, вы уже зарегистрированы.");
            }
            userStates.put(chatId, "");
        }

        else if (text.equals("Вход")) {
            sendText(chatId, "Введите пароль:");
            userStates.put(chatId, "LOGIN_PASSWORD");
        }

        else if (state.equals("LOGIN_PASSWORD")) {
            boolean valid = UserDao.validateUser(chatId, text);
            if (valid) {
                sendText(chatId, "✅ Вход выполнен.");
                showDaysKeyboard(chatId);
                userStates.put(chatId, "LOGGED_IN");
            } else {
                sendText(chatId, "❌ Неверный пароль.");
                userStates.put(chatId, "");
            }
        }

        else if (state.equals("LOGGED_IN") && isWeekday(text)) {
            String group = UserDao.getUserGroup(chatId);
            if (group != null) {
                String schedule = ScheduleDao.getSchedule(group, text);
                sendText(chatId, schedule);
            } else {
                sendText(chatId, "⚠️ Не удалось определить вашу группу.");
            }
        }

        else {
            sendText(chatId, "❓ Неизвестная команда. Напишите /start");
        }
    }

    private void sendMainMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Привет! Выберите действие:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Регистрация"));
        row1.add(new KeyboardButton("Вход"));
        rows.add(row1);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDaysKeyboard(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите день недели или нажмите ⛔ Стоп для выхода:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(new KeyboardRow(List.of(
                new KeyboardButton("Понедельник"),
                new KeyboardButton("Вторник")
        )));
        rows.add(new KeyboardRow(List.of(
                new KeyboardButton("Среда"),
                new KeyboardButton("Четверг")
        )));
        rows.add(new KeyboardRow(List.of(
                new KeyboardButton("Пятница")
        )));
        rows.add(new KeyboardRow(List.of(
                new KeyboardButton("⛔ Стоп")
        )));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendText(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isWeekday(String text) {
        return List.of("Понедельник", "Вторник", "Среда", "Четверг", "Пятница").contains(text);
    }

    @Override
    public void onRegister() {
        List<BotCommand> commandList = List.of(
                new BotCommand("/start", "Запуск бота"),
                new BotCommand("/update_schedule", "Обновить расписание (админ)"),
                new BotCommand("/send_message", "Отправить сообщение (админ)")
        );

        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(commandList);
        setMyCommands.setScope(new BotCommandScopeDefault());
        setMyCommands.setLanguageCode("ru");

        try {
            this.execute(setMyCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
