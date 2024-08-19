package com.example.wahoo;

import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.StrUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * @author lizezhong
 * @date 2024/8/19 14:24
 * @description
 */
public class StravaCallbackServer {

    private int port = 8080;

    public StravaCallbackServer(int port) {
        this.port = port;
    }

    public void start(Consumer<String> authSuccessCallback) throws IOException {
        final HttpServer httpServer = HttpServer.create(new InetSocketAddress(this.port), 1);
        httpServer.createContext("/index", t -> {
            String response = "<a href=\"https://www.strava.com/oauth/authorize?client_id=132299&response_type=code&redirect_uri=http://8.210.88.156:" + port + "/strava/callback&scope=activity:read,activity:write\">strava auth</a>";
            writeResponse(t, response);
        });

        httpServer.createContext("/strava/callback", t -> {
            final CharSequence code = UrlQuery.of(t.getRequestURI().getQuery(), StandardCharsets.UTF_8).get("code");
            if (StrUtil.isBlank(code)) {
                writeResponse(t, "error, code is empty!");
            } else {
                writeResponse(t, "success, code is " + code);
                authSuccessCallback.accept(code.toString());
            }
        });
        httpServer.start();
    }

    private void writeResponse(HttpExchange t, String response) throws IOException {
        t.getResponseHeaders().set("Content-Type", "text/html");
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}
