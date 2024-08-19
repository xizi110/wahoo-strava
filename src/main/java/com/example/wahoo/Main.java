package com.example.wahoo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * wahoo app 不能直接把活动上传到strava，但是可以分享到邮件，邮件附件是.fit文件。
 * strava支持上传.fit文件，所以本程序就是自动将邮件中的文件上传到strava，不用手动操作。
 *
 * @author lizezhong
 * @date 2024/8/15 14:08
 * @description
 */
public class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Properties props = new Properties();

    public static void main(String[] args) throws IOException {
        loadConfig();
        final int port = Integer.parseInt(props.getProperty("server.port", "8088"));
        final int initialDelay = Integer.parseInt(props.getProperty("refreshToken.initialDelay.hours", "5"));
        final int period = Integer.parseInt(props.getProperty("refreshToken.period.hours", "5"));

        StravaApi stravaApi = StravaApi.getInstance();
        final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        new StravaCallbackServer(port)
                .start(new Consumer<String>() {
                    boolean wahooEmailIsRunning = false;
                    @Override
                    public void accept(String code) {
                        stravaApi.getAccessToken(code);
                        scheduledExecutor.scheduleAtFixedRate(stravaApi::refreshToken, initialDelay, period, TimeUnit.HOURS);
                        if (!wahooEmailIsRunning) {
                            LOGGER.info("WahooEmailRunnable is running");
                            scheduledExecutor.scheduleAtFixedRate(new WahooEmailRunnable(), 0, 30, TimeUnit.MINUTES);
                            wahooEmailIsRunning = true;
                        }
                    }
                });
        LOGGER.info("strava callback server start at " + port);
    }

    private static void loadConfig() throws IOException {
        final File file = new File(System.getProperty("user.dir"), "application.properties");
        if (file.exists()) {
            props.load(Files.newInputStream(file.toPath()));
        } else {
            LOGGER.warn("application.properties file is not exists");
        }
    }
}