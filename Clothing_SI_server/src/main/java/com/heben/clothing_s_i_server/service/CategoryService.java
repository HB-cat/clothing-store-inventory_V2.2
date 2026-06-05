package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.Category;
import com.heben.clothing_s_i_server.entity.Product;
import com.heben.clothing_s_i_server.mapper.CategoryMapper;
import com.heben.clothing_s_i_server.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    /**
     * 获取用户的分类列表（含每个分类的商品数量）
     */
    public List<CategoryVO> getCategories(Long userId) {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .orderByAsc(Category::getSortOrder)
        );

        // 统计每个分类下的商品数
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getUserId, userId)
                        .eq(Product::getIsDeleted, 0)
        );
        Map<Long, Long> productCountMap = products.stream()
                .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.counting()));

        return categories.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(c.getId());
            vo.setUserId(c.getUserId());
            vo.setName(c.getName());
            vo.setSortOrder(c.getSortOrder());
            vo.setIsPreset(c.getIsPreset());
            vo.setProductCount(productCountMap.getOrDefault(c.getId(), 0L).intValue());
            vo.setCreatedAt(c.getCreatedAt());
            vo.setUpdatedAt(c.getUpdatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 新增分类
     */
    public Category addCategory(Long userId, String name) {
        // 获取当前最大 sort_order
        Category maxOrderCat = categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .orderByDesc(Category::getSortOrder)
                        .last("LIMIT 1")
        );
        int nextOrder = (maxOrderCat == null) ? 1 : maxOrderCat.getSortOrder() + 1;

        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setSortOrder(nextOrder);
        category.setIsPreset(0);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 修改分类名称
     */
    public void updateCategory(Long userId, Long categoryId, String name) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new IllegalArgumentException("分类不存在");
        }
        category.setName(name);
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    /**
     * 删除分类
     */
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new IllegalArgumentException("分类不存在");
        }

        // 检查分类下是否有商品
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getCategoryId, categoryId)
                        .eq(Product::getUserId, userId)
                        .eq(Product::getIsDeleted, 0)
        );
        if (productCount > 0) {
            throw new IllegalArgumentException("该分类下有 " + productCount + " 个商品，请先转移或删除");
        }

        categoryMapper.deleteById(categoryId);
    }

    /**
     * 批量更新排序
     */
    @Transactional
    public void updateSort(Long userId, List<SortItem> sortItems) {
        for (SortItem item : sortItems) {
            Category category = categoryMapper.selectById(item.getId());
            if (category != null && category.getUserId().equals(userId)) {
                category.setSortOrder(item.getSortOrder());
                category.setUpdatedAt(LocalDateTime.now());
                categoryMapper.updateById(category);
            }
        }
    }

    // ========== 内部类 ==========

    @lombok.Data
    public static class CategoryVO {
        private Long id;
        private Long userId;
        private String name;
        private Integer sortOrder;
        private Integer isPreset;
        private Integer productCount; // 该分类下的商品数量
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @lombok.Data
    public static class SortItem {
        private Long id;
        private Integer sortOrder;
    }
}
