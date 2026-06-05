package com.heben.clothing_s_i_server.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.*;
import com.heben.clothing_s_i_server.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final CategoryMapper categoryMapper;
    private final AttributeGroupMapper attributeGroupMapper;
    private final AttributeValueMapper attributeValueMapper;
    private final NotificationSettingMapper notificationSettingMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
    }

    private void initAdminUser() {
        SysUser admin = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "heben")
        );
        if (admin != null) {
            log.info("管理员账号已存在，跳过初始化");
            return;
        }

        SysUser user = new SysUser();
        user.setUsername("heben");
        user.setPassword(passwordEncoder.encode("heben0831"));
        user.setRole(4);
        user.setShopName("系统管理员");
        user.setStatus(1);
        user.setAppointmentBanCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);
        log.info("管理员账号已创建：heben");
    }

    public void initPresetDataForNewUser(Long userId) {
        String[] presetCategories = {"连衣裙类", "T恤类", "衬衫类", "裤子类", "外套类"};
        for (int i = 0; i < presetCategories.length; i++) {
            Category category = new Category();
            category.setUserId(userId);
            category.setName(presetCategories[i]);
            category.setSortOrder(i + 1);
            category.setIsPreset(1);
            category.setCreatedAt(LocalDateTime.now());
            category.setUpdatedAt(LocalDateTime.now());
            categoryMapper.insert(category);
        }

        initAttributeGroupWithValues(userId, "风格", List.of("休闲", "商务", "运动"));
        initAttributeGroupWithValues(userId, "面料", List.of("棉", "麻", "涤纶"));
        initAttributeGroupWithValues(userId, "颜色", List.of("红色", "白色", "黑色", "蓝色", "绿色"));
        initAttributeGroupWithValues(userId, "尺码", List.of("S", "M", "L", "XL"));
        initAttributeGroupWithValues(userId, "季节", List.of("春", "夏", "秋", "冬"));

        NotificationSetting setting = new NotificationSetting();
        setting.setUserId(userId);
        setting.setCreatedAt(LocalDateTime.now());
        setting.setUpdatedAt(LocalDateTime.now());
        notificationSettingMapper.insert(setting);

        log.info("用户 {} 预置数据初始化完成", userId);
    }

    private void initAttributeGroupWithValues(Long userId, String groupName, List<String> values) {
        AttributeGroup group = new AttributeGroup();
        group.setUserId(userId);
        group.setName(groupName);
        group.setIsPreset(1);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        attributeGroupMapper.insert(group);

        for (int i = 0; i < values.size(); i++) {
            AttributeValue value = new AttributeValue();
            value.setGroupId(group.getId());
            value.setValue(values.get(i));
            value.setSortOrder(i + 1);
            value.setCreatedAt(LocalDateTime.now());
            attributeValueMapper.insert(value);
        }
    }
}
