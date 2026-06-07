package com.heben.clothing_s_i_server.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String shopName;
}
