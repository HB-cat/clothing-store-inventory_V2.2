package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.Category;
import com.heben.clothing_s_i_server.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表
     * GET /api/categories
     */
    @GetMapping
    public ApiResponse<List<CategoryService.CategoryVO>> getCategories(
            @RequestAttribute("currentUserId") Long userId) {
        List<CategoryService.CategoryVO> list = categoryService.getCategories(userId);
        return ApiResponse.success(list);
    }

    /**
     * 新增分类
     * POST /api/categories
     */
    @PostMapping
    public ApiResponse<Category> addCategory(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ApiResponse.badRequest("分类名称不能为空");
        }
        try {
            Category category = categoryService.addCategory(userId, name);
            return ApiResponse.success("新增成功", category);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 修改分类
     * PUT /api/categories/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateCategory(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ApiResponse.badRequest("分类名称不能为空");
        }
        try {
            categoryService.updateCategory(userId, id, name);
            return ApiResponse.success("修改成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 删除分类
     * DELETE /api/categories/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        try {
            categoryService.deleteCategory(userId, id);
            return ApiResponse.success("删除成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 批量更新排序
     * PUT /api/categories/sort
     */
    @PutMapping("/sort")
    public ApiResponse<Void> updateSort(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody List<CategoryService.SortItem> sortItems) {
        categoryService.updateSort(userId, sortItems);
        return ApiResponse.success("排序已更新", null);
    }
}
