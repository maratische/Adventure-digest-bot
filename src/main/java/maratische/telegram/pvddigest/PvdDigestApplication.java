package maratische.telegram.pvddigest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@EnableScheduling
@SpringBootApplication
public class PvdDigestApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        var ctx = SpringApplication.run(PvdDigestApplication.class, args);
        SettingsUtil.Companion.getAppProps();
        var aa = 0;
        while (aa != 1) {
            aa = System.in.read();
            System.out.println(aa);
            Thread.sleep(1000);
        }
        ctx.close();
    }

}
