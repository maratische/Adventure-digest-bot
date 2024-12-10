package maratische.telegram.pvddigest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PvdDigestApplicationTests {

    @Test
    void contextLoads() {
        var telegramService = new TelegramClient();
        //PvdDigest_dev2_bot
        telegramService.setSecret(SettingsUtil.Companion.loadTelegramKeySecondBot());
        telegramService.getAllTelegramUpdates();
        telegramService.sendMessage("-1002179941104", "Привет! это тестовый процесс", "", null);
    }

}
