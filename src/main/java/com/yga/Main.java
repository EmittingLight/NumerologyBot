package com.yga;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        NumerologyBot numerologyBot = new NumerologyBot();
        numerologyBot.userDataMap = new HashMap<>();

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

        try {
            telegramBotsApi.registerBot(numerologyBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
