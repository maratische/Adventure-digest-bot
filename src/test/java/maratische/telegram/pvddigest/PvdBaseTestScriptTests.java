package maratische.telegram.pvddigest;

import java.util.Random;

public class PvdBaseTestScriptTests {
    public static void main(String[] args) throws InterruptedException {
        var telegramService = new TelegramClient();
        //PvdDigest_dev2_bot
        telegramService.setProcessGetUpdatesItem((item) -> {
            System.out.println("");
        });
        telegramService.setSecret(SettingsUtil.Companion.loadTelegramKeySecondBot());
        var chatId = "-1002179941104";
        var rand = Integer.toString(new Random().nextInt(1000));
        telegramService.sendMessage(chatId, "Привет! это тестовый процесс " + rand);
        Thread.sleep(10000);
        telegramService.sendMessage(chatId, "Пошли в поход. #pvd  " + rand);
    }
}
