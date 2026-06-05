package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.Appointment;
import com.heben.clothing_s_i_server.entity.SysUser;
import com.heben.clothing_s_i_server.mapper.AppointmentMapper;
import com.heben.clothing_s_i_server.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final SysUserMapper sysUserMapper;

    private static final List<String> VALID_TIME_SLOTS = Arrays.asList("9-11", "11-13", "14-16", "16-18");

    @Transactional
    public Appointment create(Long buyerId, Long sellerId, LocalDate appointmentDate, String timeSlot) {
        SysUser seller = sysUserMapper.selectById(sellerId);
        if (seller == null) throw new IllegalArgumentException("卖家不存在");
        if (seller.getStatus() == 0) throw new IllegalArgumentException("该卖家已被禁用");
        if (seller.getRole() != 1 && seller.getRole() != 2) throw new IllegalArgumentException("该用户不是商家，无法预约");
        if (appointmentDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("预约日期不能是过去");
        if (!VALID_TIME_SLOTS.contains(timeSlot)) throw new IllegalArgumentException("非法时段，可选：9-11, 11-13, 14-16, 16-18");

        Long exist = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getBuyerId, buyerId)
                        .eq(Appointment::getSellerId, sellerId)
                        .eq(Appointment::getAppointmentDate, appointmentDate)
                        .eq(Appointment::getTimeSlot, timeSlot)
                        .in(Appointment::getStatus, 0, 1)
        );
        if (exist > 0) throw new IllegalArgumentException("您已预约过该时段，请勿重复预约");

        Appointment appointment = new Appointment();
        appointment.setBuyerId(buyerId);
        appointment.setSellerId(sellerId);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setTimeSlot(timeSlot);
        appointment.setStatus(0);
        appointment.setReminderSent(0);
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentMapper.insert(appointment);
        return appointment;
    }

    public List<Appointment> list(Long userId, Integer role, Integer status) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        if (role == 3) {
            wrapper.eq(Appointment::getBuyerId, userId);
        } else {
            wrapper.eq(Appointment::getSellerId, userId);
        }
        if (status != null) wrapper.eq(Appointment::getStatus, status);
        wrapper.orderByDesc(Appointment::getCreatedAt);
        return appointmentMapper.selectList(wrapper);
    }

    @Transactional
    public void confirm(Long userId, Long appointmentId) {
        Appointment appointment = getAndCheckSeller(userId, appointmentId);
        if (appointment.getStatus() != 0) throw new IllegalArgumentException("仅待确认的预约可以确认");
        appointment.setStatus(1);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
    }

    @Transactional
    public void reject(Long userId, Long appointmentId, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("拒绝理由不能为空");
        Appointment appointment = getAndCheckSeller(userId, appointmentId);
        if (appointment.getStatus() != 0) throw new IllegalArgumentException("仅待确认的预约可以拒绝");
        appointment.setStatus(3);
        appointment.setRejectReason(reason);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
    }

    @Transactional
    public void complete(Long userId, Long appointmentId) {
        Appointment appointment = getAndCheckSeller(userId, appointmentId);
        if (appointment.getStatus() != 1) throw new IllegalArgumentException("仅已确认的预约可以标记完成");
        appointment.setStatus(2);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
    }

    @Transactional
    public void cancel(Long userId, Long appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !appointment.getBuyerId().equals(userId)) throw new IllegalArgumentException("预约不存在");
        if (appointment.getStatus() != 0 && appointment.getStatus() != 1) throw new IllegalArgumentException("仅待确认或已确认的预约可以取消");
        appointment.setStatus(4);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
    }

    @Transactional
    public void breach(Long userId, Long appointmentId) {
        Appointment appointment = getAndCheckSeller(userId, appointmentId);
        if (appointment.getStatus() != 1) throw new IllegalArgumentException("仅已确认的预约可以标记毁约");
        appointment.setStatus(5);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        SysUser seller = sysUserMapper.selectById(userId);
        int count = (seller.getAppointmentBanCount() == null ? 0 : seller.getAppointmentBanCount()) + 1;
        seller.setAppointmentBanCount(count);
        if (count >= 6) {
            seller.setAppointmentBanUntil(LocalDateTime.now().plusMonths(1));
        }
        sysUserMapper.updateById(seller);
    }

    private Appointment getAndCheckSeller(Long userId, Long appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !appointment.getSellerId().equals(userId)) throw new IllegalArgumentException("预约不存在");
        return appointment;
    }
}
