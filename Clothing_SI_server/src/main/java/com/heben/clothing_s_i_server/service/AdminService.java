package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.Appointment;
import com.heben.clothing_s_i_server.entity.Product;
import com.heben.clothing_s_i_server.entity.SysUser;
import com.heben.clothing_s_i_server.mapper.AppointmentMapper;
import com.heben.clothing_s_i_server.mapper.ProductMapper;
import com.heben.clothing_s_i_server.mapper.SaleRecordMapper;
import com.heben.clothing_s_i_server.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SysUserMapper sysUserMapper;
    private final ProductMapper productMapper;
    private final SaleRecordMapper saleRecordMapper;
    private final AppointmentMapper appointmentMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUser login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null || user.getRole() != 4) return null;
        if (!passwordEncoder.matches(password, user.getPassword())) return null;
        return user;
    }

    public List<SysUser> listUsers(String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SysUser::getRole, 4);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysUser::getUsername, keyword);
        }
        wrapper.orderByAsc(SysUser::getId);
        return sysUserMapper.selectList(wrapper);
    }

    public void toggleUserStatus(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getRole() == 4) return;
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    public Map<String, Object> getUserStats() {
        List<SysUser> allUsers = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().ne(SysUser::getRole, 4)
        );

        int total = allUsers.size();
        int retailers = (int) allUsers.stream().filter(u -> u.getRole() == 1).count();
        int wholesalers = (int) allUsers.stream().filter(u -> u.getRole() == 2).count();
        int buyers = (int) allUsers.stream().filter(u -> u.getRole() == 3).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("retailers", retailers);
        stats.put("wholesalers", wholesalers);
        stats.put("buyers", buyers);
        return stats;
    }

    public List<Map<String, Object>> getDataRanking() {
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().ne(SysUser::getRole, 4)
        );

        return users.stream().map(u -> {
            Long productCount = productMapper.selectCount(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getUserId, u.getId())
                            .eq(Product::getIsDeleted, 0)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getId());
            item.put("username", u.getUsername());
            String displayName = u.getShopName() != null ? u.getShopName() : u.getNickname();
            item.put("displayName", displayName != null ? displayName : "—");
            item.put("role", u.getRole());
            item.put("productCount", productCount);
            item.put("status", u.getStatus());
            return item;
        }).sorted((a, b) -> Long.compare(
                (Long) b.get("productCount"), (Long) a.get("productCount")
        )).collect(Collectors.toList());
    }

    @Transactional
    public void cleanupAppointments() {
        appointmentMapper.delete(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getStatus, 4)
                .lt(Appointment::getCreatedAt, LocalDateTime.now().minusDays(30)));
    }

    @Transactional
    public void cleanupTestData() {
        List<SysUser> nonAdminUsers = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().ne(SysUser::getRole, 4)
        );
        for (SysUser user : nonAdminUsers) {
            productMapper.delete(new LambdaQueryWrapper<Product>()
                    .eq(Product::getUserId, user.getId()));
        }
        sysUserMapper.deleteBatchIds(
                nonAdminUsers.stream().map(SysUser::getId).collect(Collectors.toList())
        );
    }
}
