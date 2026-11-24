package org.bitmagic.ifeed.infrastructure.oauth.liunxdo;

/**
 * @author yangrd
 * @date 2025/11/27
 **/

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class UserInfo {

    // id: 用户唯一标识（不可变）
    private Long id;

    // sub: OAuth Subject (通常是 id 的字符串形式)
    private String sub;

    // username: 论坛用户名
    private String username;

    // login: 登录名 (可能与 username 相同)
    private String login;

    // name: 论坛用户昵称（可变）
    private String name;

    // email: 邮箱地址
    private String email;

    // avatar_template: 用户头像模板URL
    @JsonProperty("avatar_template")
    private String avatarTemplate;

    // avatar_url: 完整的头像URL
    @JsonProperty("avatar_url")
    private String avatarUrl;

    // active: 账号活跃状态
    private Boolean active;

    // trust_level: 信任等级（0-4）
    @JsonProperty("trust_level")
    private Integer trustLevel;

    // silenced: 禁言状态
    private Boolean silenced;

    // external_ids: 外部ID关联信息 (使用 Map<String, Object> 接收 null 或对象)
    @JsonProperty("external_ids")
    private Map<String, Object> externalIds;

    // api_key: API访问密钥
    @JsonProperty("api_key")
    private String apiKey;

}