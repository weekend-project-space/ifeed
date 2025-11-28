package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.service.AuthService;
import org.bitmagic.ifeed.domain.service.AuthToken;
import org.bitmagic.ifeed.infrastructure.oauth.liunxdo.LinuxDoConnectService;
import org.bitmagic.ifeed.infrastructure.oauth.liunxdo.UserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yangrd
 * @date 2025/11/27
 **/

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth/linuxdo")
public class LinuxDoConnectController {

    private final LinuxDoConnectService linuxDoConnectService;

    private final AuthService authService;

    /**
     * 1. 启动授权流程
     * URL: GET /auth/linuxdo
     * 作用: 将用户重定向到 Linux.do 的授权页面。
     */
    @GetMapping
    public String startAuth() {
        String authUrl = linuxDoConnectService.generateAuthUrl();
        return authUrl;
    }

    /**
     * 2. 接收回调并处理授权码 (Code)
     * URL: GET /auth/linuxdo/callback?code=...
     * 作用: 接收授权码，完成 Token 交换，获取用户信息。
     */
    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestParam("code") String code) {
        try {
            // 换取 Token
            String accessToken = linuxDoConnectService.getAccessToken(code).getAccessToken();

            // 获取用户信息
            UserInfo userInfo = linuxDoConnectService.getUserInfo(accessToken);
            AuthToken token = authService.authLogin(userInfo.getSub(), userInfo.getUsername());

            // 示例：返回成功信息和用户信息
            return ResponseEntity.ok(
                    token
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("授权处理失败: " + e.getMessage());
        }
    }
}