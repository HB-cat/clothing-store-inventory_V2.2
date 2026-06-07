package com.heben.clothing_s_i_server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.*;
import com.heben.clothing_s_i_server.mapper.ProductAttrCombinationMapper;
import com.heben.clothing_s_i_server.mapper.ProductMapper;
import com.heben.clothing_s_i_server.mapper.SysUserMapper;
import com.heben.clothing_s_i_server.service.BuyerFollowService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/retailers")
@RequiredArgsConstructor
public class BuyerRetailerController {

    private final SysUserMapper sysUserMapper;
    private final ProductMapper productMapper;
    private final ProductAttrCombinationMapper combinationMapper;
    private final BuyerFollowService followService;

    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @RequestParam String keyword) {
        if (role != 3) return ApiResponse.forbidden("仅买家可访问");
        List<SysUser> sellers = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getRole, 1, 2)
                        .and(w -> w.like(SysUser::getShopName, keyword)
                                .or().like(SysUser::getNickname, keyword))
                        .eq(SysUser::getStatus, 1)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser seller : sellers) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", seller.getId());
            item.put("shopName", seller.getShopName());
            item.put("nickname", seller.getNickname());
            item.put("role", seller.getRole());
            item.put("isFollowing", followService.isFollowing(userId, seller.getId()));
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{sellerId}/products")
    public ApiResponse<List<Map<String, Object>>> getProducts(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @PathVariable Long sellerId) {
        if (role != 3) return ApiResponse.forbidden("仅买家可访问");
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getUserId, sellerId)
                        .eq(Product::getStatus, 1)
                        .eq(Product::getIsDeleted, 0)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("sellingPrice", p.getSellingPrice());
            item.put("imageUrl", p.getImageUrl());
            item.put("categoryId", p.getCategoryId());

            List<ProductAttrCombination> combos = combinationMapper.selectList(
                    new LambdaQueryWrapper<ProductAttrCombination>()
                            .eq(ProductAttrCombination::getProductId, p.getId())
            );
            item.put("combinations", combos.stream().map(c -> {
                Map<String, Object> cm = new HashMap<>();
                cm.put("id", c.getId());
                cm.put("attrsJson", c.getAttrsJson());
                cm.put("stock", c.getStock());
                return cm;
            }).collect(Collectors.toList()));
            result.add(item);
        }
        return ApiResponse.success(result);
    }
}
