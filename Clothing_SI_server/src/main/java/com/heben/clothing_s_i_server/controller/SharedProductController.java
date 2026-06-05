package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.SharedProductPool;
import com.heben.clothing_s_i_server.service.SharedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shared-products")
@RequiredArgsConstructor
public class SharedProductController {

    private final SharedProductService sharedProductService;

    /**
     * 批发商添加商品到共享池
     * POST /api/shared-products
     */
    @PostMapping
    public ApiResponse<SharedProductPool> addShared(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @RequestBody Map<String, Object> body) {
        if (role != 2) {
            return ApiResponse.forbidden("仅批发商可操作");
        }
        Long productId = Long.valueOf(body.get("productId").toString());
        BigDecimal sharedPrice = body.get("sharedPrice") != null
                ? new BigDecimal(body.get("sharedPrice").toString())
                : BigDecimal.ZERO;
        try {
            SharedProductPool pool = sharedProductService.addShared(userId, productId, sharedPrice);
            return ApiResponse.success("共享成功", pool);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 查看共享池商品列表
     * GET /api/shared-products?wholesalerName=王老板
     */
    @GetMapping
    public ApiResponse<List<SharedProductService.SharedProductVO>> getShared(
            @RequestAttribute("currentUserId") Long userId,
            @RequestParam(name = "wholesalerName", required = false) String wholesalerName) {
        return ApiResponse.success(sharedProductService.getSharedProducts(userId, wholesalerName));
    }

    /**
     * 取消共享
     * DELETE /api/shared-products/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeShared(
            @RequestAttribute("currentUserId") Long userId,
            @RequestAttribute("currentUserRole") Integer role,
            @PathVariable Long id) {
        if (role != 2) {
            return ApiResponse.forbidden("仅批发商可操作");
        }
        try {
            sharedProductService.removeShared(userId, id);
            return ApiResponse.success("已取消共享", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
}
