package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.BuyerPersonalInfo;
import com.heben.clothing_s_i_server.mapper.BuyerPersonalInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BuyerInfoService {

    private final BuyerPersonalInfoMapper buyerPersonalInfoMapper;

    public BuyerPersonalInfo getInfo(Long userId) {
        return buyerPersonalInfoMapper.selectOne(
                new LambdaQueryWrapper<BuyerPersonalInfo>()
                        .eq(BuyerPersonalInfo::getUserId, userId)
        );
    }

    public void saveInfo(Long userId, BuyerPersonalInfo info) {
        BuyerPersonalInfo existing = getInfo(userId);
        if (existing != null) {
            info.setId(existing.getId());
            info.setUserId(userId);
            buyerPersonalInfoMapper.updateById(info);
        } else {
            info.setId(null);
            info.setUserId(userId);
            buyerPersonalInfoMapper.insert(info);
        }
    }
}
