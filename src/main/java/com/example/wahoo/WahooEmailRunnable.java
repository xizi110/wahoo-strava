package com.example.wahoo;

import cn.hutool.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.SearchTerm;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 读取邮箱中的右键，并下载.fit文件，然后调用strava api上传接口上传活动
 * @author lizezhong
 * @date 2024/8/16 11:10
 * @description
 */
public class WahooEmailRunnable implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(WahooEmailRunnable.class);

    private static final Pattern PATTERN = Pattern.compile("https://cdn.wahooligan.com.+fit");
    private final Properties props = new Properties();

    public WahooEmailRunnable() {
        try {
            // 读取邮箱配置
            final File file = new File(System.getProperty("user.dir"), "email-config.properties");
            props.load(Files.newInputStream(file.toPath()));
        } catch (IOException e) {
            LOGGER.error("email-config.properties file is not exists");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        LOGGER.info("start pooling...");
        // 创建Session实例对象
        Session session = Session.getInstance(props);
        try {
            // 创建IMAP协议的Store对象
            Store store = session.getStore("imap");
            // 连接邮件服务器
            store.connect(props.getProperty("mail.address"), props.getProperty("mail.token"));
            Folder folder = store.getFolder("INBOX");
            // 以只读模式打开收件箱
            folder.open(Folder.READ_ONLY);
            // 搜索邮件
            // 过滤发件人 no-reply@wahooligan.com
            final FromTerm fromTerm = new FromTerm(new InternetAddress("no-reply@wahooligan.com"));
            // 邮件内容包含 download.fit
            final BodyTerm bodyTerm = new BodyTerm("download.fit");
            final FlagTerm flagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            final AndTerm andTerm = new AndTerm(new SearchTerm[]{fromTerm, bodyTerm, flagTerm});
            Message[] messages = folder.search(andTerm);

            if (messages != null && messages.length > 0) {
                LOGGER.info("有{}条未读的wahoo消息!", messages.length);
                processUnreadMessage(messages);
            } else {
                LOGGER.info("没有未读的wahoo消息!");
            }

            // 关闭资源
            folder.close(false);
            store.close();
            LOGGER.info("end pooling...");
        } catch (Exception e) {
            LOGGER.error("异常：", e);
        }
    }

    private void processUnreadMessage(Message[] messages) throws MessagingException, IOException {
        for (final Message message : messages) {
            LOGGER.info("邮件标题：{}", message.getSubject());
            if (message.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (bodyPart.isMimeType("text/plain")) {
                        final String content = String.valueOf(bodyPart.getContent());
                        System.out.println("content = " + content);
                        final Matcher matcher = PATTERN.matcher(content);
                        if (matcher.find()) {
                            final String downloadLink = matcher.group();
                            LOGGER.info("fit file download url：{}", downloadLink);
                            final File file = HttpUtil.downloadFileFromUrl(downloadLink, new File(System.getProperty("user.dir")));
                            LOGGER.info("fit file is download success. location is {}.", file.toPath());
                            final boolean uploaded = StravaApi.getInstance().uploadActivity(getActivityName(content), "", file);
                            if (uploaded) {
                                message.setFlag(Flags.Flag.SEEN, true);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getActivityName(String content) {
        final int i = content.indexOf("You recently completed");
        content = content.substring(i);
        final int i1 = content.indexOf("and");
        return content.substring(22, i1).trim();
    }
}
