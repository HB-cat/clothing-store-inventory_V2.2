package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.*;
import com.heben.clothing_s_i_server.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRecordMapper purchaseRecordMapper;
    private final PurchaseRecordItemMapper purchaseRecordItemMapper;
    private final ProductMapper productMapper;
    private final ProductAttrCombinationMapper combinationMapper;
    private final WholesaleTransactionMapper wholesaleTransactionMapper;
    private final CategoryMapper categoryMapper;
    private final SharedProductPoolMapper sharedProductPoolMapper;

    /**
     * 来源1：从我的已有商品进货（补货）
     */
    @Transactional
    public PurchaseRecord purchaseMyProduct(Long userId, Long productId, Long combinationId, int quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getUserId().equals(userId)) {
            throw new IllegalArgumentException("商品不存在");
        }

        ProductAttrCombination combo = combinationMapper.selectById(combinationId);
        if (combo == null || !combo.getProductId().equals(productId)) {
            throw new IllegalArgumentException("属性组合不存在");
        }

        combinationMapper.addStock(combinationId, quantity);

        PurchaseRecord record = new PurchaseRecord();
        record.setUserId(userId);
        record.setSource(1);
        record.setTotalAmount(product.getPurchasePrice().multiply(BigDecimal.valueOf(quantity)));
        record.setCreatedAt(LocalDateTime.now());
        purchaseRecordMapper.insert(record);

        PurchaseRecordItem item = new PurchaseRecordItem();
        item.setPurchaseId(record.getId());
        item.setProductId(productId);
        item.setCombinationId(combinationId);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPurchasePrice());
        item.setCreatedAt(LocalDateTime.now());
        purchaseRecordItemMapper.insert(item);

        return record;
    }

    /**
     * 来源2：新建商品并入库（一步完成）
     */
    @Transactional
    public Product purchaseNewProduct(Long userId, ProductService.AddProductRequest request) {
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || !category.getUserId().equals(userId)) {
            throw new IllegalArgumentException("分类不存在");
        }

        Product product = new Product();
        product.setUserId(userId);
        product.setName(request.getName());
        product.setCategoryId(request.getCategoryId());
        product.setPurchasePrice(request.getPurchasePrice() != null ? request.getPurchasePrice() : BigDecimal.ZERO);
        product.setSellingPrice(request.getSellingPrice() != null ? request.getSellingPrice() : BigDecimal.ZERO);
        product.setImageUrl(request.getImageUrl());
        product.setStatus(1);
        product.setIsDeleted(0);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(product);

        BigDecimal totalCost = BigDecimal.ZERO;
        for (ProductService.AddProductRequest.ComboItem ci : request.getCombinations()) {
            ProductAttrCombination combo = new ProductAttrCombination();
            combo.setProductId(product.getId());
            combo.setAttrsJson(ci.getAttrsJson());
            combo.setStock(ci.getStock() != null ? ci.getStock() : 0);
            combo.setVersion(0);
            combo.setCreatedAt(LocalDateTime.now());
            combo.setUpdatedAt(LocalDateTime.now());
            combinationMapper.insert(combo);

            totalCost = totalCost.add(product.getPurchasePrice().multiply(BigDecimal.valueOf(combo.getStock())));
        }

        PurchaseRecord record = new PurchaseRecord();
        record.setUserId(userId);
        record.setSource(2);
        record.setTotalAmount(totalCost);
        record.setCreatedAt(LocalDateTime.now());
        purchaseRecordMapper.insert(record);

        for (ProductService.AddProductRequest.ComboItem ci : request.getCombinations()) {
            PurchaseRecordItem item = new PurchaseRecordItem();
            item.setPurchaseId(record.getId());
            item.setProductId(product.getId());
            item.setQuantity(ci.getStock() != null ? ci.getStock() : 0);
            item.setUnitPrice(product.getPurchasePrice());
            item.setCreatedAt(LocalDateTime.now());
            purchaseRecordItemMapper.insert(item);
        }

        return product;
    }

    /**
     * 来源3：发起批零进货请求
     */
    @Transactional
    public WholesaleTransaction requestWholesale(Long retailerId, Long sharedProductId, int quantity,
                                                  BigDecimal unitPrice) {
        SharedProductPool pool = sharedProductPoolMapper.selectById(sharedProductId);
        if (pool == null) {
            throw new IllegalArgumentException("共享商品不存在");
        }

        WholesaleTransaction tx = new WholesaleTransaction();
        tx.setWholesalerId(pool.getWholesalerId());
        tx.setRetailerId(retailerId);
        tx.setProductId(pool.getProductId());
        tx.setQuantity(quantity);
        tx.setUnitPrice(unitPrice);
        tx.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        tx.setStatus(0);
        tx.setCreatedAt(LocalDateTime.now());
        wholesaleTransactionMapper.insert(tx);
        return tx;
    }

    /**
     * 批发商确认/拒绝进货请求
     */
    @Transactional
    public void confirmWholesale(Long wholesalerId, Long txId, boolean approved) {
        WholesaleTransaction tx = wholesaleTransactionMapper.selectById(txId);
        if (tx == null || !tx.getWholesalerId().equals(wholesalerId)) {
            throw new IllegalArgumentException("交易请求不存在");
        }
        if (tx.getStatus() != 0) {
            throw new IllegalArgumentException("该请求已处理");
        }

        if (approved) {
            ProductAttrCombination combo = combinationMapper.selectById(tx.getCombinationId());
            if (combo == null) {
                throw new IllegalArgumentException("商品属性组合不存在");
            }

            int affected = combinationMapper.deductStock(combo.getId(), tx.getQuantity(), combo.getVersion());
            if (affected == 0) {
                throw new IllegalArgumentException("库存不足或并发冲突");
            }

            tx.setStatus(1);
            tx.setConfirmedAt(LocalDateTime.now());
            wholesaleTransactionMapper.updateById(tx);
        } else {
            tx.setStatus(2);
            tx.setConfirmedAt(LocalDateTime.now());
            wholesaleTransactionMapper.updateById(tx);
        }
    }
}
