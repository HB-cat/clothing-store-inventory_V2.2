package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.Category;
import com.heben.clothing_s_i_server.entity.OperationLog;
import com.heben.clothing_s_i_server.entity.Product;
import com.heben.clothing_s_i_server.entity.ProductAttrCombination;
import com.heben.clothing_s_i_server.mapper.CategoryMapper;
import com.heben.clothing_s_i_server.mapper.OperationLogMapper;
import com.heben.clothing_s_i_server.mapper.ProductAttrCombinationMapper;
import com.heben.clothing_s_i_server.mapper.ProductMapper;
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
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductAttrCombinationMapper combinationMapper;
    private final CategoryMapper categoryMapper;
    private final OperationLogMapper operationLogMapper;

    /**
     * 商品列表（支持按分类筛选、名称搜索）
     */
    public List<ProductVO> getProducts(Long userId, Long categoryId, String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, userId)
                .eq(Product::getIsDeleted, 0);

        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getUpdatedAt);

        List<Product> products = productMapper.selectList(wrapper);
        if (products.isEmpty()) {
            return List.of();
        }

        // 批量查询所有属性组合
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        List<ProductAttrCombination> allCombinations = combinationMapper.selectList(
                new LambdaQueryWrapper<ProductAttrCombination>()
                        .in(ProductAttrCombination::getProductId, productIds)
        );
        Map<Long, List<ProductAttrCombination>> comboMap = allCombinations.stream()
                .collect(Collectors.groupingBy(ProductAttrCombination::getProductId));

        return products.stream().map(p -> {
            ProductVO vo = new ProductVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setCategoryId(p.getCategoryId());
            vo.setPurchasePrice(p.getPurchasePrice());
            vo.setSellingPrice(p.getSellingPrice());
            vo.setImageUrl(p.getImageUrl());
            vo.setStatus(p.getStatus());
            vo.setCreatedAt(p.getCreatedAt());

            List<ProductAttrCombination> combos = comboMap.getOrDefault(p.getId(), List.of());
            vo.setCombinations(combos);
            vo.setTotalStock(combos.stream().mapToInt(ProductAttrCombination::getStock).sum());

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 新增商品（含属性组合库存）
     */
    @Transactional
    public Product addProduct(Long userId, AddProductRequest request) {
        // 校验分类
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || !category.getUserId().equals(userId)) {
            throw new IllegalArgumentException("分类不存在");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("商品名称不能为空");
        }
        if (request.getCombinations() == null || request.getCombinations().isEmpty()) {
            throw new IllegalArgumentException("至少需要一个属性组合");
        }

        // 创建商品
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

        // 批量插入属性组合
        for (AddProductRequest.ComboItem item : request.getCombinations()) {
            ProductAttrCombination combo = new ProductAttrCombination();
            combo.setProductId(product.getId());
            combo.setAttrsJson(item.getAttrsJson());
            combo.setStock(item.getStock() != null ? item.getStock() : 0);
            combo.setVersion(0);
            combo.setCreatedAt(LocalDateTime.now());
            combo.setUpdatedAt(LocalDateTime.now());
            combinationMapper.insert(combo);
        }

        return product;
    }

    /**
     * 编辑商品
     */
    @Transactional
    public void updateProduct(Long userId, Long productId, AddProductRequest request) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getUserId().equals(userId) || product.getIsDeleted() == 1) {
            throw new IllegalArgumentException("商品不存在");
        }

        product.setName(request.getName());
        if (request.getCategoryId() != null) {
            product.setCategoryId(request.getCategoryId());
        }
        product.setPurchasePrice(request.getPurchasePrice() != null ? request.getPurchasePrice() : BigDecimal.ZERO);
        product.setSellingPrice(request.getSellingPrice() != null ? request.getSellingPrice() : BigDecimal.ZERO);
        product.setImageUrl(request.getImageUrl());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        // 删除旧的属性组合，重新插入
        combinationMapper.delete(
                new LambdaQueryWrapper<ProductAttrCombination>()
                        .eq(ProductAttrCombination::getProductId, productId)
        );

        if (request.getCombinations() != null) {
            for (AddProductRequest.ComboItem item : request.getCombinations()) {
                ProductAttrCombination combo = new ProductAttrCombination();
                combo.setProductId(productId);
                combo.setAttrsJson(item.getAttrsJson());
                combo.setStock(item.getStock() != null ? item.getStock() : 0);
                combo.setVersion(0);
                combo.setCreatedAt(LocalDateTime.now());
                combo.setUpdatedAt(LocalDateTime.now());
                combinationMapper.insert(combo);
            }
        }
    }

    /**
     * 删除商品（软删除）
     */
    public void deleteProduct(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getUserId().equals(userId) || product.getIsDeleted() == 1) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 检查库存
        List<ProductAttrCombination> combos = combinationMapper.selectList(
                new LambdaQueryWrapper<ProductAttrCombination>()
                        .eq(ProductAttrCombination::getProductId, productId)
        );
        int totalStock = combos.stream().mapToInt(ProductAttrCombination::getStock).sum();
        if (totalStock > 0) {
            throw new IllegalArgumentException("该商品还有 " + totalStock + " 件库存，请先处理再删除");
        }

        product.setIsDeleted(1);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    // ========== 内部类 ==========

    /**
     * 批量增量同步库存（离线客户端使用）
     * 幂等：operation_id 已存在则跳过
     */
    @Transactional
    public void syncStockChanges(Long userId, List<StockSyncItem> changes) {
        for (StockSyncItem item : changes) {
            // 幂等去重
            Long count = operationLogMapper.selectCount(
                    new LambdaQueryWrapper<OperationLog>()
                            .eq(OperationLog::getOperationId, item.getOperationId())
            );
            if (count > 0) continue;

            // 增量更新库存（原子加减）
            if (item.getChange() > 0) {
                combinationMapper.addStock(item.getCombinationId(), item.getChange());
            } else if (item.getChange() < 0) {
                combinationMapper.deductStockUnsafe(item.getCombinationId(), -item.getChange());
            }

            // 记录操作日志
            OperationLog log = new OperationLog();
            log.setOperationId(item.getOperationId());
            log.setUserId(userId);
            log.setOperationType("stock_sync");
            log.setStatus(1);
            log.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(log);
        }
    }

    @lombok.Data
    public static class StockSyncItem {
        private Long combinationId;
        private int change; // 正数=入库，负数=出库
        private String operationId;
    }

    @lombok.Data
    public static class ProductVO {
        private Long id;
        private String name;
        private Long categoryId;
        private BigDecimal purchasePrice;
        private BigDecimal sellingPrice;
        private String imageUrl;
        private Integer status;
        private Integer totalStock;
        private List<ProductAttrCombination> combinations;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    public static class AddProductRequest {
        private String name;
        private Long categoryId;
        private BigDecimal purchasePrice;
        private BigDecimal sellingPrice;
        private String imageUrl;
        private List<ComboItem> combinations;

        @lombok.Data
        public static class ComboItem {
            private String attrsJson;
            private Integer stock;
        }
    }
}
