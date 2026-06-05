package com.heben.clothing_s_i_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 12, message = "密码长度为8-12位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,12}$", message = "密码需包含字母和数字")
    private String newPassword;
}
