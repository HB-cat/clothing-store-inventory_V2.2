package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.*;
import com.heben.clothing_s_i_server.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRecordMapper saleRecordMapper;
    private final SaleRecordItemMapper saleRecordItemMapper;
    private final DraftBillMapper draftBillMapper;
    private final DraftBillItemMapper draftBillItemMapper;
    private final ProductAttrCombinationMapper combinationMapper;
    private final ProductMapper productMapper;
    private final BuyerInfoMapper buyerInfoMapper;

    /**
     * 快捷指令记账
     */
    @Transactional
    public SaleRecord quickSale(Long userId, QuickSaleRequest request) {
        // 处理买家
        Long buyerId = handleBuyer(userId, request.getBuyerId(), request.getBuyerName());

        // 创建销售记录
        SaleRecord record = new SaleRecord();
        record.setUserId(userId);
        record.setBuyerId(buyerId);
        record.setBuyerName(request.getBuyerName() != null ? request.getBuyerName() : "散客");
        record.setSaleTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());

        // 统计总金额和数量
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (QuickSaleItem item : request.getItems()) {
            ProductAttrCombination combo = combinationMapper.selectById(item.getCombinationId());
            if (combo == null) {
                throw new IllegalArgumentException("商品属性组合不存在: " + item.getCombinationId());
            }
            totalAmount = totalAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        record.setTotalAmount(totalAmount);
        record.setItemCount(request.getItems().size());
        saleRecordMapper.insert(record);

        // 插入明细并扣库存
        for (QuickSaleItem item : request.getItems()) {
            // 查询组合和商品信息
            ProductAttrCombination combo = combinationMapper.selectById(item.getCombinationId());
            Product product = productMapper.selectById(combo.getProductId());

            // 乐观锁扣库存
            int affected = combinationMapper.deductStock(combo.getId(), item.getQuantity(), combo.getVersion());
            if (affected == 0) {
                throw new IllegalArgumentException("库存不足或并发冲突: " + combo.getAttrsJson());
            }

            // 插入明细
            SaleRecordItem saleItem = new SaleRecordItem();
            saleItem.setSaleId(record.getId());
            saleItem.setProductId(product.getId());
            saleItem.setProductName(product.getName());
            saleItem.setCombinationId(combo.getId());
            saleItem.setAttrsJson(combo.getAttrsJson());
            saleItem.setQuantity(item.getQuantity());
            saleItem.setUnitPrice(item.getUnitPrice());
            saleItem.setPurchasePrice(product.getPurchasePrice());
            saleItem.setIsReturned(0);
            saleItem.setCreatedAt(LocalDateTime.now());
            saleRecordItemMapper.insert(saleItem);
        }

        return record;
    }

    // ========== 草稿账单 ==========

    /**
     * 创建草稿账单
     */
    public DraftBill createDraftBill(Long userId) {
        // 检查未提交的草稿数
        long pendingCount = draftBillMapper.selectCount(
                new LambdaQueryWrapper<DraftBill>()
                        .eq(DraftBill::getUserId, userId)
                        .eq(DraftBill::getStatus, 0)
        );
        if (pendingCount >= 6) {
            throw new IllegalArgumentException("最多6个未提交草稿");
        }

        // 分配序号
        DraftBill bill = new DraftBill();
        bill.setUserId(userId);
        bill.setBillIndex((int) pendingCount + 1);
        bill.setTotalAmount(BigDecimal.ZERO);
        bill.setItemCount(0);
        bill.setStatus(0);
        bill.setCreatedAt(LocalDateTime.now());
        bill.setUpdatedAt(LocalDateTime.now());
        draftBillMapper.insert(bill);
        return bill;
    }

    /**
     * 获取草稿列表
     */
    public List<DraftBillVO> getDraftBills(Long userId) {
        List<DraftBill> bills = draftBillMapper.selectList(
                new LambdaQueryWrapper<DraftBill>()
                        .eq(DraftBill::getUserId, userId)
                        .eq(DraftBill::getStatus, 0)
                        .orderByAsc(DraftBill::getBillIndex)
        );
        return bills.stream().map(b -> {
            DraftBillVO vo = new DraftBillVO();
            vo.setId(b.getId());
            vo.setBillIndex(b.getBillIndex());
            vo.setTotalAmount(b.getTotalAmount());
            vo.setItemCount(b.getItemCount());
            vo.setCreatedAt(b.getCreatedAt());

            List<DraftBillItem> items = draftBillItemMapper.selectList(
                    new LambdaQueryWrapper<DraftBillItem>()
                            .eq(DraftBillItem::getBillId, b.getId())
            );
            vo.setItems(items);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 草稿加商品
     */
    @Transactional
    public DraftBillItem addItemToDraft(Long userId, Long billId, DraftBillItem item) {
        DraftBill bill = draftBillMapper.selectById(billId);
        if (bill == null || !bill.getUserId().equals(userId) || bill.getStatus() == 1) {
            throw new IllegalArgumentException("草稿账单不存在或已提交");
        }

        // 检查商品数
        long currentCount = draftBillItemMapper.selectCount(
                new LambdaQueryWrapper<DraftBillItem>().eq(DraftBillItem::getBillId, billId)
        );
        if (currentCount >= 30) {
            throw new IllegalArgumentException("每张草稿最多30种商品");
        }

        item.setBillId(billId);
        item.setCreatedAt(LocalDateTime.now());
        draftBillItemMapper.insert(item);

        // 更新草稿统计
        bill.setItemCount(bill.getItemCount() + 1);
        bill.setTotalAmount(bill.getTotalAmount().add(
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        ));
        bill.setUpdatedAt(LocalDateTime.now());
        draftBillMapper.updateById(bill);

        return item;
    }

    /**
     * 删除草稿账单及其商品
     */
    @Transactional
    public void deleteDraftBill(Long userId, Long billId) {
        DraftBill bill = draftBillMapper.selectById(billId);
        if (bill == null || !bill.getUserId().equals(userId)) {
            throw new IllegalArgumentException("草稿账单不存在");
        }
        draftBillItemMapper.delete(
                new LambdaQueryWrapper<DraftBillItem>().eq(DraftBillItem::getBillId, billId)
        );
        draftBillMapper.deleteById(billId);
    }

    /**
     * 草稿删商品
     */
    @Transactional
    public void removeItemFromDraft(Long userId, Long billId, Long itemId) {
        DraftBill bill = draftBillMapper.selectById(billId);
        if (bill == null || !bill.getUserId().equals(userId) || bill.getStatus() == 1) {
            throw new IllegalArgumentException("草稿账单不存在或已提交");
        }

        DraftBillItem item = draftBillItemMapper.selectById(itemId);
        if (item == null || !item.getBillId().equals(billId)) {
            throw new IllegalArgumentException("商品不存在");
        }

        draftBillItemMapper.deleteById(itemId);

        // 更新草稿统计
        bill.setItemCount(bill.getItemCount() - 1);
        bill.setTotalAmount(bill.getTotalAmount().subtract(
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        ));
        bill.setUpdatedAt(LocalDateTime.now());
        draftBillMapper.updateById(bill);
    }

    /**
     * 提交草稿 → 生成正式销售记录
     */
    @Transactional
    public SaleRecord submitDraft(Long userId, Long billId, String buyerName, Long buyerId) {
        DraftBill bill = draftBillMapper.selectById(billId);
        if (bill == null || !bill.getUserId().equals(userId) || bill.getStatus() == 1) {
            throw new IllegalArgumentException("草稿账单不存在或已提交");
        }

        List<DraftBillItem> items = draftBillItemMapper.selectList(
                new LambdaQueryWrapper<DraftBillItem>().eq(DraftBillItem::getBillId, billId)
        );
        if (items.isEmpty()) {
            throw new IllegalArgumentException("草稿为空，无法提交");
        }

        // 处理买家
        Long finalBuyerId = handleBuyer(userId, buyerId, buyerName);

        // 创建销售记录
        SaleRecord record = new SaleRecord();
        record.setUserId(userId);
        record.setBuyerId(finalBuyerId);
        record.setBuyerName(buyerName != null ? buyerName : "散客");
        record.setTotalAmount(bill.getTotalAmount());
        record.setItemCount(items.size());
        record.setDraftBillId(billId);
        record.setSaleTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        saleRecordMapper.insert(record);

        // 逐条扣库存 + 生成明细
        for (DraftBillItem item : items) {
            ProductAttrCombination combo = combinationMapper.selectById(item.getCombinationId());
            if (combo != null) {
                int affected = combinationMapper.deductStock(combo.getId(), item.getQuantity(), combo.getVersion());
                if (affected == 0) {
                    throw new IllegalArgumentException("库存不足或并发冲突: " + combo.getAttrsJson());
                }
            }

            Product product = productMapper.selectById(item.getProductId());

            SaleRecordItem saleItem = new SaleRecordItem();
            saleItem.setSaleId(record.getId());
            saleItem.setProductId(item.getProductId());
            saleItem.setProductName(product != null ? product.getName() : "未知商品");
            saleItem.setCombinationId(item.getCombinationId());
            saleItem.setAttrsJson(item.getAttrsJson());
            saleItem.setQuantity(item.getQuantity());
            saleItem.setUnitPrice(item.getUnitPrice());
            saleItem.setPurchasePrice(product != null ? product.getPurchasePrice() : BigDecimal.ZERO);
            saleItem.setIsReturned(0);
            saleItem.setCreatedAt(LocalDateTime.now());
            saleRecordItemMapper.insert(saleItem);
        }

        // 标记草稿为已提交
        bill.setStatus(1);
        bill.setUpdatedAt(LocalDateTime.now());
        draftBillMapper.updateById(bill);

        return record;
    }

    // ========== 账单查询 ==========

    /**
     * 今日销售
     */
    public SaleSummaryVO getTodaySales(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        return getSalesSummary(userId, start, end);
    }

    /**
     * 本月销售
     */
    public SaleSummaryVO getMonthSales(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDateTime start = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = now.atTime(LocalTime.MAX);

        return getSalesSummary(userId, start, end);
    }

    /**
     * 本年销售
     */
    public SaleSummaryVO getYearSales(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDateTime start = now.withDayOfYear(1).atStartOfDay();
        LocalDateTime end = now.atTime(LocalTime.MAX);
        return getSalesSummary(userId, start, end);
    }

    /**
     * 按日期范围查询销售统计
     */
    public SaleSummaryVO getSalesByDateRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return getSalesSummary(userId, start, end);
    }

    // ========== 退货 ==========

    /**
     * 退货
     */
    @Transactional
    public void returnItem(Long userId, Long saleItemId) {
        SaleRecordItem item = saleRecordItemMapper.selectById(saleItemId);
        if (item == null) {
            throw new IllegalArgumentException("销售明细不存在");
        }

        SaleRecord record = saleRecordMapper.selectById(item.getSaleId());
        if (record == null || !record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("销售记录不存在");
        }

        if (item.getIsReturned() == 1) {
            throw new IllegalArgumentException("已退货，不能重复退");
        }

        // 标记退货
        item.setIsReturned(1);
        saleRecordItemMapper.updateById(item);

        // 库存加回
        combinationMapper.addStock(item.getCombinationId(), item.getQuantity());
    }

    // ========== 私有方法 ==========

    private SaleSummaryVO getSalesSummary(Long userId, LocalDateTime start, LocalDateTime end) {
        List<SaleRecord> records = saleRecordMapper.selectList(
                new LambdaQueryWrapper<SaleRecord>()
                        .eq(SaleRecord::getUserId, userId)
                        .ge(SaleRecord::getSaleTime, start)
                        .le(SaleRecord::getSaleTime, end)
                        .orderByDesc(SaleRecord::getSaleTime)
        );

        SaleSummaryVO vo = new SaleSummaryVO();
        vo.setRecords(records);

        if (records.isEmpty()) {
            vo.setTotalAmount(BigDecimal.ZERO);
            vo.setTotalProfit(BigDecimal.ZERO);
            vo.setOrderCount(0);
            return vo;
        }

        // 统计总金额
        BigDecimal totalAmount = records.stream()
                .map(SaleRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalAmount(totalAmount);
        vo.setOrderCount(records.size());

        // 查询所有明细并分组
        List<Long> saleIds = records.stream().map(SaleRecord::getId).collect(Collectors.toList());
        List<SaleRecordItem> allItems = saleRecordItemMapper.selectList(
                new LambdaQueryWrapper<SaleRecordItem>()
                        .in(SaleRecordItem::getSaleId, saleIds)
        );
        Map<Long, List<SaleRecordItem>> itemsBySaleId = allItems.stream()
                .collect(Collectors.groupingBy(SaleRecordItem::getSaleId));

        // 为每条记录填充明细
        for (SaleRecord r : records) {
            r.setSaleItems(itemsBySaleId.getOrDefault(r.getId(), new ArrayList<>()));
        }

        // 统计毛利（排除已退货）
        BigDecimal totalCost = allItems.stream()
                .filter(i -> i.getIsReturned() == 0)
                .map(i -> i.getPurchasePrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalProfit(totalAmount.subtract(totalCost));

        return vo;
    }

    private Long handleBuyer(Long userId, Long buyerId, String buyerName) {
        if (buyerId != null) {
            return buyerId;
        }
        if (buyerName == null || buyerName.isBlank() || "散客".equals(buyerName)) {
            return null;
        }
        // 简易：按姓名查找或创建
        BuyerInfo buyer = buyerInfoMapper.selectOne(
                new LambdaQueryWrapper<BuyerInfo>()
                        .eq(BuyerInfo::getSellerId, userId)
                        .eq(BuyerInfo::getBuyerName, buyerName)
        );
        if (buyer == null) {
            buyer = new BuyerInfo();
            buyer.setSellerId(userId);
            buyer.setBuyerName(buyerName);
            buyer.setVisitCount(1);
            buyer.setLastVisitTime(LocalDateTime.now());
            buyer.setCreatedAt(LocalDateTime.now());
            buyer.setUpdatedAt(LocalDateTime.now());
            buyerInfoMapper.insert(buyer);
        } else {
            buyer.setVisitCount(buyer.getVisitCount() + 1);
            buyer.setLastVisitTime(LocalDateTime.now());
            buyer.setUpdatedAt(LocalDateTime.now());
            buyerInfoMapper.updateById(buyer);
        }
        return buyer.getId();
    }

    /**
     * 按日期范围查询销售流水明细（含明细商品）
     */
    public List<SaleRecord> getSalesRecords(Long userId, LocalDateTime start, LocalDateTime end) {
        List<SaleRecord> records = saleRecordMapper.selectList(
                new LambdaQueryWrapper<SaleRecord>()
                        .eq(SaleRecord::getUserId, userId)
                        .ge(SaleRecord::getSaleTime, start)
                        .le(SaleRecord::getSaleTime, end)
                        .orderByDesc(SaleRecord::getSaleTime)
        );
        if (records.isEmpty()) return records;

        List<Long> saleIds = records.stream().map(SaleRecord::getId).collect(Collectors.toList());
        List<SaleRecordItem> allItems = saleRecordItemMapper.selectList(
                new LambdaQueryWrapper<SaleRecordItem>()
                        .in(SaleRecordItem::getSaleId, saleIds)
        );
        Map<Long, List<SaleRecordItem>> itemsBySaleId = allItems.stream()
                .collect(Collectors.groupingBy(SaleRecordItem::getSaleId));

        for (SaleRecord r : records) {
            r.setSaleItems(itemsBySaleId.getOrDefault(r.getId(), new ArrayList<>()));
        }
        return records;
    }

    /**
     * 买家历史订单
     */
    public List<SaleRecord> getBuyerOrders(Long buyerUserId) {
        return saleRecordMapper.selectList(
                new LambdaQueryWrapper<SaleRecord>()
                        .eq(SaleRecord::getBuyerId, buyerUserId)
                        .orderByDesc(SaleRecord::getSaleTime)
        );
    }

    // ========== 内部类 ==========

    @lombok.Data
    public static class QuickSaleRequest {
        private Long buyerId;
        private String buyerName;
        private List<QuickSaleItem> items;
    }

    @lombok.Data
    public static class QuickSaleItem {
        private Long combinationId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }

    @lombok.Data
    public static class DraftBillVO {
        private Long id;
        private Integer billIndex;
        private BigDecimal totalAmount;
        private Integer itemCount;
        private LocalDateTime createdAt;
        private List<DraftBillItem> items;
    }

    @lombok.Data
    public static class SaleSummaryVO {
        private BigDecimal totalAmount;
        private BigDecimal totalProfit;
        @com.fasterxml.jackson.annotation.JsonProperty("totalCount")
        private Integer orderCount;
        private List<SaleRecord> records;
    }
}
