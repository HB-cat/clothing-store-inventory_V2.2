package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.Product;
import com.heben.clothing_s_i_server.entity.SharedProductPool;
import com.heben.clothing_s_i_server.service.ProductService;
import com.heben.clothing_s_i_server.service.ProductService.StockSyncItem;
import com.heben.clothing_s_i_server.service.SharedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 商品列表
     * GET /api/products?categoryId=1&keyword=连衣裙
     */
    @GetMapping
    public ApiResponse<List<ProductService.ProductVO>> getProducts(
            @RequestAttribute("currentUserId") Long userId,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ApiResponse.success(productService.getProducts(userId, categoryId, keyword));
    }

    /**
     * 新增商品
     * POST /api/products
     */
    @PostMapping
    public ApiResponse<Product> addProduct(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody ProductService.AddProductRequest request) {
        try {
            Product product = productService.addProduct(userId, request);
            return ApiResponse.success("新增商品成功", product);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 编辑商品
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateProduct(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @RequestBody ProductService.AddProductRequest request) {
        try {
            productService.updateProduct(userId, id, request);
            return ApiResponse.success("修改成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 删除商品
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        try {
            productService.deleteProduct(userId, id);
            return ApiResponse.success("删除成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 批量增量同步库存（离线客户端使用，幂等）
     * POST /api/products/stock-sync
     */
    @PostMapping("/stock-sync")
    public ApiResponse<Void> syncStockChanges(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, List<StockSyncItem>> body) {
        try {
            productService.syncStockChanges(userId, body.get("changes"));
            return ApiResponse.success("库存同步完成", null);
        } catch (Exception e) {
            return ApiResponse.badRequest("库存同步失败: " + e.getMessage());
        }
    }
}
