package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.Product;
import com.heben.clothing_s_i_server.entity.PurchaseRecord;
import com.heben.clothing_s_i_server.entity.WholesaleTransaction;
import com.heben.clothing_s_i_server.service.ProductService;
import com.heben.clothing_s_i_server.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final ProductService productService;

    /**
     * 来源1：从我的商品进货
     * POST /api/purchases/my-products
     */
    @PostMapping("/my-products")
    public ApiResponse<PurchaseRecord> purchaseMyProduct(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        Long combinationId = Long.valueOf(body.get("combinationId").toString());
        int quantity = Integer.parseInt(body.get("quantity").toString());

        try {
            PurchaseRecord record = purchaseService.purchaseMyProduct(userId, productId, combinationId, quantity);
            return ApiResponse.success("进货成功", record);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 来源2：新建商品并入库
     * POST /api/purchases/new-product
     */
    @PostMapping("/new-product")
    public ApiResponse<Product> purchaseNewProduct(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody ProductService.AddProductRequest request) {
        try {
            Product product = purchaseService.purchaseNewProduct(userId, request);
            return ApiResponse.success("新建入库成功", product);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 来源3：发起批零进货请求（零售商发起）
     * POST /api/purchases/wholesale-request
     */
    @PostMapping("/wholesale-request")
    public ApiResponse<WholesaleTransaction> requestWholesale(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long sharedProductId = Long.valueOf(body.get("sharedProductId").toString());
        int quantity = Integer.parseInt(body.get("quantity").toString());
        BigDecimal unitPrice = new BigDecimal(body.get("unitPrice").toString());

        try {
            WholesaleTransaction tx = purchaseService.requestWholesale(userId, sharedProductId, quantity, unitPrice);
            return ApiResponse.success("进货请求已发送", tx);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 批发商确认进货请求
     * PUT /api/purchases/wholesale-confirm/{id}
     */
    @PutMapping("/wholesale-confirm/{id}")
    public ApiResponse<Void> confirmWholesale(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        boolean approved = body.get("approved") == null || Boolean.parseBoolean(body.get("approved").toString());
        try {
            purchaseService.confirmWholesale(userId, id, approved);
            return ApiResponse.success(approved ? "已确认" : "已拒绝", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
}
