package org.bitmagic.ifeed.infrastructure.oauth.liunxdo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * @author yangrd
 * @date 2025/11/27
 **/
@Component
public class LinuxDoConnectClient {

    // 从配置文件读取
    @Value("${app.oauth.linux-do.client-id}")
    private String clientId;

    @Value("${app.oauth.linux-do.client-secret}")
    private String clientSecret;

    @Value("${app.oauth.linux-do.redirect-uri}")
    private String redirectUri;

    private static final String AUTH_URL = "https://connect.linux.do/oauth2/authorize";
    private static final String TOKEN_URL = "https://connect.linux.do/oauth2/token";
    private static final String USER_INFO_URL = "https://connect.linux.do/api/user";

    // 使用 Java 11+ HttpClient 和 Jackson 库 (需添加依赖)
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 第一步：生成授权链接
     * 对应 Node.js 中的 getAuthUrl()
     */
    public String generateAuthUrl() {
        String params = String.format(
                "client_id=%s&redirect_uri=%s&response_type=code&scope=user",
                urlEncode(clientId),
                urlEncode(redirectUri));
        return AUTH_URL + "?" + params;
    }

    /**
     * 第二步：使用授权码获取访问令牌
     * 对应 Node.js 中的 getAccessToken(code)
     */
    public TokenResponse getAccessToken(String code) throws Exception {
        String requestBody = String.format(
                "client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&grant_type=authorization_code",
                urlEncode(clientId),
                urlEncode(clientSecret),
                urlEncode(code),
                urlEncode(redirectUri));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("获取访问令牌失败. Status: " + response.statusCode() + ", Body: " + response.body());
        }

        // 使用 Jackson 库将 JSON 字符串反序列化为 DTO
        return objectMapper.readValue(response.body(), TokenResponse.class);
    }

    /**
     * 第三步：使用访问令牌获取用户信息
     * 对应 Node.js 中的 getUserInfo(accessToken)
     */
    public UserInfo getUserInfo(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_INFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("获取用户信息失败. Status: " + response.statusCode() + ", Body: " + response.body());
        }

        // 使用 Jackson 库将 JSON 字符串反序列化为 UserInfo DTO
        return objectMapper.readValue(response.body(), UserInfo.class);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new RuntimeException("URL encoding failed", e);
        }
    }
}
