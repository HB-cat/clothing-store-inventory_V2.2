package com.heben.clothing_s_i_server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private LocalDateTime tokenExpire;
    private UserInfo user;

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String shopName;
        private String nickname;
        private Integer role;
    }
}
