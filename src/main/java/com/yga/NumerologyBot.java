package com.yga;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NumerologyBot extends TelegramLongPollingBot {
    private boolean isWaitingForInfo = false;

    private static final String START_COMMAND = "/start";
    private static final String CONTINUE_COMMAND = "/continue";
    private static final String NAME_PROMPT = "Введите свою фамилию,имя и дату рождения вот так: Фамилия Имя ДД.ММ.ГГГГ.";
    private static final String INVALID_DATE_MESSAGE = "Неверный формат даты рождения. Пожалуйста, используйте формат ДД.ММ.ГГГГ";
    private static final String NUMEROLOGY_RESULT_MESSAGE = "Ваши числа раскрыты! Подготовьтесь к открытиям:\n" +
            "Число вашего имени и фамилии: %d\n" +
            "Расшифровка числа вашего имени и фамилии: %s\n" +
            "Число вашей даты рождения: %d\n" +
            "Расшифровка числа вашей даты рождения: %s";
    private static final String INVALID_FORMAT_MESSAGE = "Неверный формат. Пожалуйста, используйте формат Фамилия Имя ДД.ММ.ГГГГ.";
    public Map<Long, String> userDataMap;
    public NumerologyBot() {
        userDataMap = new HashMap<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.equals(START_COMMAND)) {
                sendMessageWithButton(chatId, "Приветствую! Я - бот-нумеролог, " +
                        "и у меня есть уникальная способность расчета чисел вашего имени и даты рождения согласно нумерологическим правилам. " +
                        "Готовы узнать свои числа? Просто нажмите кнопку \"Продолжить\", и мы начнем погружение в мир чисел и их тайн.", "Продолжить");
            } else if (messageText.equals(CONTINUE_COMMAND)) {
                sendMessage(chatId, NAME_PROMPT);
            } else if (isWaitingForInfo) {
                if (messageText.equals("Получить подробную информацию")) {
                    compareNumbersAndShowInfo(chatId);
                    String userData = userDataMap.get(chatId);
                    if (userData != null) {
                        String[] parts = userData.split("\n");
                        if (parts.length == 2) {
                            int nameNumber = Integer.parseInt(parts[0]);
                            int birthDateNumber = Integer.parseInt(parts[1]);

                            String nameNumberInfo = decodeNumber(nameNumber);
                            String birthDateNumberInfo = decodeNumber(birthDateNumber);

                            String details = "Число вашего имени и фамилии: " + nameNumber + " - " + nameNumberInfo + "\n" +
                                    "Число вашей даты рождения: " + birthDateNumber + " - " + birthDateNumberInfo;

                            sendMessage(chatId, details);
                            sendMessageWithButton(chatId, "Закончить работу", "Закончить");
                        }
                    }
                } else if (messageText.equals("Закончить")) {
                    userDataMap.remove(chatId);
                    isWaitingForInfo = false;

                    sendMessage(chatId, "Спасибо за использование нашего бота! Надеемся, что вам понравился наш нумерологический расчет.");

                    // Отправить сообщение с начальным приветствием и кнопкой "Продолжить"
                    sendMessageWithButton(chatId, "Добро пожаловать! Числовая магия ждет вас! " +
                            "Раскройте тайны своих чисел, введя свою фамилию, имя и дату рождения в формате: " +
                            "Фамилия Имя ДД.ММ.ГГГГ. Я расшифрую истинную сущность вашей жизни и открою перед вами магический мир чисел. " +
                            "Погрузимся в путешествие числовой магии вместе!", "Продолжить");
                }

            } else {
                String userData = userDataMap.get(chatId);

                if (userData == null) {
                    String[] parts = messageText.split(" ");

                    if (parts.length < 3) {
                        sendMessage(chatId, NAME_PROMPT);
                        return;
                    }

                    String firstName = parts[0];
                    String lastName = parts[1];
                    String dateOfBirthStr = parts[2];

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    LocalDate dateOfBirth;

                    try {
                        dateOfBirth = LocalDate.parse(dateOfBirthStr, formatter);
                    } catch (DateTimeParseException e) {
                        sendMessage(chatId, INVALID_DATE_MESSAGE);
                        return;
                    }

                    int nameNumber;

                    try {
                        nameNumber = calculateNameNumber(firstName, lastName);
                    } catch (IllegalArgumentException e) {
                        sendMessage(chatId, "Неверный символ: " + e.getMessage());
                        sendMessage(chatId, NAME_PROMPT);
                        return;
                    }

                    int birthDateNumber = calculateBirthDateNumber(dateOfBirth);

                    String numerologyResult = String.format(NUMEROLOGY_RESULT_MESSAGE, nameNumber,
                            decodeNumber(nameNumber), birthDateNumber, decodeNumber(birthDateNumber));

                    sendMessageWithButtons(chatId, numerologyResult, "Получить подробную информацию", "Закончить");


                    // Сохраняем данные пользователя
                    userDataMap.put(chatId, nameNumber + "\n" + birthDateNumber);

                    // Сохраняем данные пользователя в файл
                    saveDataToFile(chatId, nameNumber + "\n" + birthDateNumber);
                    isWaitingForInfo = true;

                } else {
                    sendMessage(chatId, "Выберите одну из опций, чтобы получить дополнительную информацию.");
                }
            }
        }
    }

    private void compareNumbersAndShowInfo(Long chatId) {
        try {
            String decodingFile = "target/decoding.txt";
            String infoFile = "target/info.txt";

            List<String> decodingLines = Files.readAllLines(Paths.get(decodingFile), StandardCharsets.UTF_8);
            List<String> infoLines = Files.readAllLines(Paths.get(infoFile), StandardCharsets.UTF_8);

            List<String> matchingInfo = new ArrayList<>();

            for (String decodingLine : decodingLines) {
                int decodingNumber = Integer.parseInt(decodingLine.trim());
                for (String infoLine : infoLines) {
                    String[] parts = infoLine.split(":");
                    if (parts.length == 2) {
                        int infoNumber = Integer.parseInt(parts[0].trim());
                        if (decodingNumber == infoNumber) {
                            String info = parts[1].trim();
                            matchingInfo.add(infoNumber + ": " + info);
                        }
                    }
                }
            }

            if (!matchingInfo.isEmpty()) {
                for (String info : matchingInfo) {
                    sendMessage(chatId, info);
                }
            } else {
                sendMessage(chatId, "Информация не найдена.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDataToFile(Long chatId, String data) {
        try {
            String fileName = "target/decoding.txt";
            Path filePath = Paths.get(fileName);
            Files.writeString(filePath, data, StandardCharsets.UTF_8);


            System.out.println("Данные успешно сохранены в файл.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithButton(Long chatId, String message, String buttonText) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        KeyboardButton button = new KeyboardButton();
        button.setText(buttonText);

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(button);

        List<KeyboardRow> keyboard = List.of(keyboardRow);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(keyboard);

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithButtons(Long chatId, String message, String button1Text, String button2Text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        KeyboardButton button1 = new KeyboardButton();
        button1.setText(button1Text);

        KeyboardButton button2 = new KeyboardButton();
        button2.setText(button2Text);

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(button1);

        KeyboardRow keyboardRow2 = new KeyboardRow();
        keyboardRow2.add(button2);

        List<KeyboardRow> keyboard = List.of(keyboardRow1, keyboardRow2);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(keyboard);

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static int calculateNameNumber(String firstName, String lastName) {
        int total = 0;

        // Удаляем пробелы и приводим к верхнему регистру
        firstName = firstName.replaceAll(" ", "").toUpperCase();
        lastName = lastName.replaceAll(" ", "").toUpperCase();

        String fullName = firstName + lastName;

        for (int i = 0; i < fullName.length(); i++) {
            char ch = fullName.charAt(i);

            // Проверяем, является ли символ буквой алфавита
            if (Character.isLetter(ch)) {
                int value = ch - 'A' + 1;
                total += value;
            } else {
                throw new IllegalArgumentException(String.valueOf(ch));
            }
        }

        // Свернуть числовое значение до однозначного числа
        while (total > 9) {
            total = calculateSum(total);
        }

        return total;
    }


    public static int calculateBirthDateNumber(LocalDate dateOfBirth) {
        int total = 0;

        int day = dateOfBirth.getDayOfMonth();
        int month = dateOfBirth.getMonthValue();
        int year = dateOfBirth.getYear();

        // Суммируем день, месяц и год рождения
        total = calculateSum(day) + calculateSum(month) + calculateSum(year);

        // Свернуть числовое значение до однозначного числа
        while (total > 9) {
            total = calculateSum(total);
        }

        return total;
    }

    public static int calculateSum(int number) {
        int sum = 0;

        while (number != 0) {
            sum += number % 10;
            number /= 10;
        }

        return sum;
    }

    public static String decodeNumber(int number) {
        Map<Integer, String> decodingMap = new HashMap<>();
        decodingMap.put(1, "Одиночество, лидерство, независимость");
        decodingMap.put(2, "Сотрудничество, гармония, дипломатия");
        decodingMap.put(3, "Креативность, коммуникация, веселье");
        decodingMap.put(4, "Стабильность, надежность, трудолюбие");
        decodingMap.put(5, "Свобода, приключения, переменчивость");
        decodingMap.put(6, "Гармония, семья, ответственность");
        decodingMap.put(7, "Духовность, мудрость, анализ");
        decodingMap.put(8, "Власть, успех, материальное благосостояние");
        decodingMap.put(9, "Доброта, сострадание, творчество");

        return decodingMap.get(number);
    }


    @Override
    public String getBotUsername() {
        return "NumerologyBot";
    }

    @Override
    public String getBotToken() {
        return "";
    }
}