package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.Product;
import com.heben.clothing_s_i_server.entity.ProductAttrCombination;
import com.heben.clothing_s_i_server.entity.SharedProductPool;
import com.heben.clothing_s_i_server.entity.SysUser;
import com.heben.clothing_s_i_server.mapper.ProductMapper;
import com.heben.clothing_s_i_server.mapper.SharedProductPoolMapper;
import com.heben.clothing_s_i_server.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedProductService {

    private final SharedProductPoolMapper sharedProductPoolMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 批发商添加商品到共享池
     */
    @Transactional
    public SharedProductPool addShared(Long wholesalerId, Long productId, BigDecimal sharedPrice) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getUserId().equals(wholesalerId)) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (product.getIsDeleted() == 1) {
            throw new IllegalArgumentException("商品已删除");
        }

        // 检查是否已分享过
        SharedProductPool existing = sharedProductPoolMapper.selectOne(
                new LambdaQueryWrapper<SharedProductPool>()
                        .eq(SharedProductPool::getWholesalerId, wholesalerId)
                        .eq(SharedProductPool::getProductId, productId)
        );
        if (existing != null) {
            throw new IllegalArgumentException("该商品已在共享池中");
        }

        SharedProductPool pool = new SharedProductPool();
        pool.setWholesalerId(wholesalerId);
        pool.setProductId(productId);
        pool.setSharedPrice(sharedPrice);
        pool.setCreatedAt(LocalDateTime.now());
        sharedProductPoolMapper.insert(pool);
        return pool;
    }

    /**
     * 查看共享池商品列表（零售商视角）
     */
    public List<SharedProductVO> getSharedProducts(Long userId, String wholesalerName) {
        LambdaQueryWrapper<SharedProductPool> wrapper = new LambdaQueryWrapper<>();
        // 如果是批发商，只看自己的；如果是零售商，看所有
        // 简单处理：先查出所有共享记录
        if (wholesalerName != null && !wholesalerName.isBlank()) {
            List<SysUser> wholesalers = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getRole, 2)
                            .like(SysUser::getShopName, wholesalerName)
            );
            if (!wholesalers.isEmpty()) {
                wrapper.in(SharedProductPool::getWholesalerId,
                        wholesalers.stream().map(SysUser::getId).collect(Collectors.toList()));
            }
        }
        wrapper.orderByDesc(SharedProductPool::getCreatedAt);

        List<SharedProductPool> pools = sharedProductPoolMapper.selectList(wrapper);
        if (pools.isEmpty()) {
            return List.of();
        }

        // 批量查询商品和批发商信息
        List<Long> productIds = pools.stream().map(SharedProductPool::getProductId).collect(Collectors.toList());
        List<Long> wholesalerIds = pools.stream().map(SharedProductPool::getWholesalerId).collect(Collectors.toList());

        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(wholesalerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        return pools.stream().map(p -> {
            SharedProductVO vo = new SharedProductVO();
            vo.setId(p.getId());
            vo.setProductId(p.getProductId());
            vo.setSharedPrice(p.getSharedPrice());
            vo.setCreatedAt(p.getCreatedAt());

            Product product = productMap.get(p.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setSellingPrice(product.getSellingPrice());
            }

            SysUser user = userMap.get(p.getWholesalerId());
            if (user != null) {
                vo.setWholesalerName(user.getShopName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 取消共享
     */
    public void removeShared(Long wholesalerId, Long sharedId) {
        SharedProductPool pool = sharedProductPoolMapper.selectById(sharedId);
        if (pool == null || !pool.getWholesalerId().equals(wholesalerId)) {
            throw new IllegalArgumentException("共享记录不存在");
        }
        sharedProductPoolMapper.deleteById(sharedId);
    }

    // ========== 内部类 ==========

    @lombok.Data
    public static class SharedProductVO {
        private Long id;
        private Long productId;
        private String productName;
        private BigDecimal sellingPrice;
        private BigDecimal sharedPrice;
        private String wholesalerName;
        private LocalDateTime createdAt;
    }
}
