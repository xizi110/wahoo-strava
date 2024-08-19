package com.example.wahoo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * strava api，使用oauth2认证方式请求
 */
public class StravaApi {

    public static final Logger LOGGER = LoggerFactory.getLogger(StravaApi.class);

    // 获取code
    // https://www.strava.com/oauth/authorize?client_id=132299&response_type=code&redirect_uri=htpp://127.0.0.1&scope=activity:read,activity:write
    public static String accessToken;
    public static String refreshToken;

    public static final String CLIENT_ID = "132299";
    public static final String CLIENT_SECRET = "ec7e1d6d8181c06da8e7356f61d4ed91c5158f0a";

    private static final StravaApi INSTANCE = new StravaApi();

    private StravaApi() {
    }

    public static StravaApi getInstance() {
        return INSTANCE;
    }

    /**
     * 活动上传
     * @param name 活动名
     * @param description 活动描述
     * @param file .fit格式的文件
     * @return 上传是否成功
     */
    public boolean uploadActivity(String name, String description, File file) {
        boolean result = false;
        LOGGER.info("start upload activity {}...", name);
        String url = "https://www.strava.com/api/v3/uploads";
        try (
                HttpResponse response = HttpRequest.post(url)
                        .bearerAuth(accessToken)
                        .form("file", file)
                        .form("name", name)
                        .form("description", description)
                        .form("trainer", "false")
                        .form("commute", "false")
                        .form("data_type", "fit")
                        .form("external_id", "")
                        .execute()) {
            String body = response.body();
            if (response.getStatus() != 201) {
                LOGGER.error("upload activity is failed, status is {}", response.getStatus());
            } else {
                result = true;
                LOGGER.info("upload activity is success");
            }
            LOGGER.info("upload activity is end, response result is {}", body);
        }
        return result;
    }

    /**
     * 查询活动
     */
    public void getActivities() {
        String url = "https://www.strava.com/api/v3/athlete/activities?page=1&per_page=10";
        try (HttpResponse response = HttpRequest.get(url).bearerAuth(accessToken).execute()) {
            String body = response.body();
            LOGGER.info("activities response result is {}", body);
        }
    }

    /**
     * 根据code获取access token
     * @param code 请求https://www.strava.com/oauth/authorize?client_id=132299&response_type=code&redirect_uri=htpp://127.0.0.1&scope=activity:read,activity:write
     *             获取code
     */
    public void getAccessToken(String code) {
        LOGGER.info("getAccessToken code is {}", code);
        String grantType = "authorization_code";
        String url = StrUtil.format("https://www.strava.com/api/v3/oauth/token?client_id={}&client_secret={}&grant_type={}&code={}",
                CLIENT_ID, CLIENT_SECRET, grantType, code);
        try (HttpResponse response = HttpRequest.post(url).setSSLProtocol("TLSv1.2").execute()) {
            final String responseBody = response.body();
            LOGGER.info("getAccessToken response body is {}", responseBody);
            parseResponse(responseBody);
        }

    }

    /**
     * 使用refresh token刷新access token
     */
    public void refreshToken() {
        LOGGER.info("refresh token...");
        String grantType = "refresh_token";
        String url = StrUtil.format("https://www.strava.com/api/v3/oauth/token?client_id={}&client_secret={}&grant_type={}&refresh_token={}",
                CLIENT_ID, CLIENT_SECRET, grantType, refreshToken);
        try (HttpResponse response = HttpRequest.post(url).setSSLProtocol("TLSv1.2").execute()) {
            final String responseBody = response.body();
            LOGGER.info("refreshToken response body is {}", responseBody);
            parseResponse(responseBody);
        }
    }

    private static void parseResponse(String responseBody) {
        JSONObject jsonObject = JSONObject.parseObject(responseBody);
        refreshToken = jsonObject.getString("refresh_token");
        accessToken = jsonObject.getString("access_token");
        LOGGER.info("parseResponse refresh token is {}, access token is {}", refreshToken, accessToken);
    }
}