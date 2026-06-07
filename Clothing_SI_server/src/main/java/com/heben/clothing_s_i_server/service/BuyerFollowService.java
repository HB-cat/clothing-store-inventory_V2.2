package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.BuyerFollow;
import com.heben.clothing_s_i_server.entity.SysUser;
import com.heben.clothing_s_i_server.mapper.BuyerFollowMapper;
import com.heben.clothing_s_i_server.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuyerFollowService {

    private final BuyerFollowMapper followMapper;
    private final SysUserMapper sysUserMapper;

    public void follow(Long buyerId, Long sellerId) {
        if (buyerId.equals(sellerId)) throw new IllegalArgumentException("不能关注自己");
        Long count = followMapper.selectCount(
                new LambdaQueryWrapper<BuyerFollow>()
                        .eq(BuyerFollow::getBuyerId, buyerId)
                        .eq(BuyerFollow::getSellerId, sellerId)
        );
        if (count > 0) return; // 已关注
        BuyerFollow follow = new BuyerFollow();
        follow.setBuyerId(buyerId);
        follow.setSellerId(sellerId);
        followMapper.insert(follow);
    }

    public void unfollow(Long buyerId, Long sellerId) {
        followMapper.delete(
                new LambdaQueryWrapper<BuyerFollow>()
                        .eq(BuyerFollow::getBuyerId, buyerId)
                        .eq(BuyerFollow::getSellerId, sellerId)
        );
    }

    public List<SysUser> getFollowedSellers(Long buyerId) {
        List<BuyerFollow> follows = followMapper.selectList(
                new LambdaQueryWrapper<BuyerFollow>()
                        .eq(BuyerFollow::getBuyerId, buyerId)
        );
        if (follows.isEmpty()) return new ArrayList<>();
        List<Long> sellerIds = follows.stream()
                .map(BuyerFollow::getSellerId)
                .collect(Collectors.toList());
        return sysUserMapper.selectBatchIds(sellerIds);
    }

    public boolean isFollowing(Long buyerId, Long sellerId) {
        return followMapper.selectCount(
                new LambdaQueryWrapper<BuyerFollow>()
                        .eq(BuyerFollow::getBuyerId, buyerId)
                        .eq(BuyerFollow::getSellerId, sellerId)
        ) > 0;
    }
}
