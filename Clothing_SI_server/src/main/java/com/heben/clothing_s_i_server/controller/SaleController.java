package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.DraftBill;
import com.heben.clothing_s_i_server.entity.DraftBillItem;
import com.heben.clothing_s_i_server.entity.SaleRecord;
import com.heben.clothing_s_i_server.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    // ========== 快捷记账 ==========

    /**
     * 快捷指令记账
     * POST /api/sales/quick
     */
    @PostMapping("/sales/quick")
    public ApiResponse<SaleRecord> quickSale(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody SaleService.QuickSaleRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ApiResponse.badRequest("至少需要一个商品");
        }
        try {
            SaleRecord record = saleService.quickSale(userId, request);
            return ApiResponse.success("记账成功", record);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    // ========== 草稿账单 ==========

    /**
     * 创建草稿账单
     * POST /api/draft-bills
     */
    @PostMapping("/draft-bills")
    public ApiResponse<DraftBill> createDraftBill(
            @RequestAttribute("currentUserId") Long userId) {
        try {
            DraftBill bill = saleService.createDraftBill(userId);
            return ApiResponse.success("创建草稿成功", bill);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 获取草稿列表
     * GET /api/draft-bills
     */
    @GetMapping("/draft-bills")
    public ApiResponse<List<SaleService.DraftBillVO>> getDraftBills(
            @RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(saleService.getDraftBills(userId));
    }

    /**
     * 草稿加商品
     * POST /api/draft-bills/{id}/items
     */
    @PostMapping("/draft-bills/{id}/items")
    public ApiResponse<DraftBillItem> addDraftItem(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @RequestBody DraftBillItem item) {
        try {
            DraftBillItem result = saleService.addItemToDraft(userId, id, item);
            return ApiResponse.success("已添加", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 草稿删商品
     * DELETE /api/draft-bills/{id}/items/{itemId}
     */
    @DeleteMapping("/draft-bills/{id}/items/{itemId}")
    public ApiResponse<Void> removeDraftItem(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @PathVariable Long itemId) {
        try {
            saleService.removeItemFromDraft(userId, id, itemId);
            return ApiResponse.success("已删除", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 提交草稿
     * POST /api/draft-bills/{id}/submit
     */
    @PostMapping("/draft-bills/{id}/submit")
    public ApiResponse<SaleRecord> submitDraft(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        String buyerName = body != null ? (String) body.get("buyerName") : null;
        Long buyerId = body != null && body.get("buyerId") != null
                ? Long.valueOf(body.get("buyerId").toString()) : null;
        try {
            SaleRecord record = saleService.submitDraft(userId, id, buyerName, buyerId);
            return ApiResponse.success("提交成功", record);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    // ========== 账单查询 ==========

    /**
     * 今日销售
     * GET /api/sales/today
     */
    @GetMapping("/sales/today")
    public ApiResponse<SaleService.SaleSummaryVO> getTodaySales(
            @RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(saleService.getTodaySales(userId));
    }

    /**
     * 本月销售
     * GET /api/sales/month
     */
    @GetMapping("/sales/month")
    public ApiResponse<SaleService.SaleSummaryVO> getMonthSales(
            @RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(saleService.getMonthSales(userId));
    }

    // ========== 退货 ==========

    /**
     * 退货
     * POST /api/sales/return
     */
    @PostMapping("/sales/return")
    public ApiResponse<Void> returnItem(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, Long> body) {
        Long saleItemId = body.get("saleItemId");
        if (saleItemId == null) {
            return ApiResponse.badRequest("saleItemId 不能为空");
        }
        try {
            saleService.returnItem(userId, saleItemId);
            return ApiResponse.success("退货成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
}
