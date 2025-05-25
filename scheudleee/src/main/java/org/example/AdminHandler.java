package org.example;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.*;

public class AdminHandler {

    // ✅ список админов — можно добавить несколько ID
    private static final Set<Long> ADMIN_IDS = Set.of(
            1237259277L // 🔁 заменено на твой Telegram ID
    );

    private static final Map<Long, String> state = new HashMap<>();
    private static final Map<Long, String> selectedGroup = new HashMap<>();
    private static final Map<Long, String> selectedDay = new HashMap<>();

    public static boolean handleAdmin(MyBot bot, Message msg) {
        long chatId = msg.getChatId();
        String text = msg.getText();

        // Только админ может выполнять эти действия
        if (!ADMIN_IDS.contains(chatId)) {
            return false;
        }

        String s = state.getOrDefault(chatId, "");

        if (text.equals("/admin")) {
            send(bot, chatId, "Введите группу (например, comfci-23):");
            state.put(chatId, "ADMIN_GROUP");
            return true;
        }

        if (s.equals("ADMIN_GROUP")) {
            selectedGroup.put(chatId, text.trim());
            send(bot, chatId, "Введите день недели (например, Понедельник):");
            state.put(chatId, "ADMIN_DAY");
            return true;
        }

        if (s.equals("ADMIN_DAY")) {
            selectedDay.put(chatId, text.trim());
            send(bot, chatId, "Введите новое расписание:");
            state.put(chatId, "ADMIN_TEXT");
            return true;
        }

        if (s.equals("ADMIN_TEXT")) {
            String group = selectedGroup.get(chatId);
            String day = selectedDay.get(chatId);
            String newSchedule = text.trim();

            // Обновляем расписание и уведомляем студентов (внутри метода)
            ScheduleDao.updateSchedule(group, day, newSchedule, bot);

            send(bot, chatId, "✅ Расписание обновлено и студенты уведомлены.");
            state.put(chatId, "");
            return true;
        }

        return false; // если не команда админа
    }

    private static void send(MyBot bot, long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            bot.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
