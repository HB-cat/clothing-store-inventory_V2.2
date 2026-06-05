package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.config.DataInitializer;
import com.heben.clothing_s_i_server.dto.*;
import com.heben.clothing_s_i_server.entity.*;
import com.heben.clothing_s_i_server.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final DataInitializer dataInitializer;

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    /**
     * 注册新用户
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 1. 角色校验
        Integer role = request.getRole();
        if (role == null || role < 1 || role > 3) {
            throw new IllegalArgumentException("角色值无效：1=零售商, 2=批发商, 3=买家");
        }

        // 2. 角色必填字段校验
        if (role == 3) {
            // 买家
            if (request.getNickname() == null || request.getNickname().isBlank()) {
                throw new IllegalArgumentException("买家昵称不能为空");
            }
        } else {
            // 零售商/批发商
            if (request.getShopName() == null || request.getShopName().isBlank()) {
                throw new IllegalArgumentException("店铺名称不能为空");
            }
        }

        // 3. 生成唯一用户名
        String rolePrefix;
        if (role == 1) rolePrefix = "ret";
        else if (role == 2) rolePrefix = "whl";
        else rolePrefix = "buy";

        String username = generateUniqueUsername(rolePrefix);

        // 4. 加密密码
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 5. 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRole(role);
        if (role == 3) {
            user.setNickname(request.getNickname());
        } else {
            user.setShopName(request.getShopName());
        }
        user.setStatus(1);
        user.setAppointmentBanCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        sysUserMapper.insert(user);

        // 6. 初始化预置数据（分类、属性组、通知设置）
        if (role == 1 || role == 2) {
            dataInitializer.initPresetDataForNewUser(user.getId());
        }

        // 7. 返回登录响应（注册后自动登录）
        String token = UUID.randomUUID().toString();
        LocalDateTime tokenExpire = LocalDateTime.now().plusDays(30);
        user.setToken(token);
        user.setTokenExpire(tokenExpire);
        sysUserMapper.updateById(user);

        return buildLoginResponse(user, token, tokenExpire);
    }

    /**
     * 登录
     */
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())
        );

        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 生成新Token
        String token = UUID.randomUUID().toString();
        LocalDateTime tokenExpire = LocalDateTime.now().plusDays(30);
        user.setToken(token);
        user.setTokenExpire(tokenExpire);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        return buildLoginResponse(user, token, tokenExpire);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    /**
     * Token验证
     */
    public SysUser validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getToken, token)
                        .gt(SysUser::getTokenExpire, LocalDateTime.now())
        );
        return user;
    }

    // ========== 私有方法 ==========

    private String generateUniqueUsername(String prefix) {
        Random random = new Random();
        String username;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(prefix + "_");
            for (int i = 0; i < 5; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            username = sb.toString();
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("无法生成唯一用户名，请重试");
            }
        } while (sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        ) > 0);
        return username;
    }

    private LoginResponse buildLoginResponse(SysUser user, String token, LocalDateTime tokenExpire) {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getShopName(),
                user.getNickname(),
                user.getRole()
        );
        return new LoginResponse(token, tokenExpire, userInfo);
    }
}
