package com.heben.clothing_s_i_server.controller;

import com.heben.clothing_s_i_server.dto.ApiResponse;
import com.heben.clothing_s_i_server.entity.AttributeGroup;
import com.heben.clothing_s_i_server.entity.AttributeValue;
import com.heben.clothing_s_i_server.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    /**
     * 获取属性组列表（含属性值）
     * GET /api/attributes/groups
     */
    @GetMapping("/groups")
    public ApiResponse<List<AttributeService.GroupVO>> getGroups(
            @RequestAttribute("currentUserId") Long userId) {
        return ApiResponse.success(attributeService.getGroups(userId));
    }

    /**
     * 新增属性组
     * POST /api/attributes/groups
     */
    @PostMapping("/groups")
    public ApiResponse<AttributeGroup> addGroup(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ApiResponse.badRequest("属性组名称不能为空");
        }
        AttributeGroup group = attributeService.addGroup(userId, name);
        return ApiResponse.success("新增成功", group);
    }

    /**
     * 删除属性组
     * DELETE /api/attributes/groups/{id}
     */
    @DeleteMapping("/groups/{id}")
    public ApiResponse<Void> deleteGroup(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        try {
            attributeService.deleteGroup(userId, id);
            return ApiResponse.success("删除成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 批量添加属性值
     * POST /api/attributes/values
     */
    @PostMapping("/values")
    public ApiResponse<List<AttributeValue>> addValues(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long groupId = Long.valueOf(body.get("groupId").toString());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) body.get("values");
        if (groupId == null || values == null || values.isEmpty()) {
            return ApiResponse.badRequest("参数不完整");
        }
        try {
            List<AttributeValue> result = attributeService.addValues(userId, groupId, values);
            return ApiResponse.success("添加成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    /**
     * 删除属性值
     * DELETE /api/attributes/values/{id}
     */
    @DeleteMapping("/values/{id}")
    public ApiResponse<Void> deleteValue(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        try {
            attributeService.deleteValue(userId, id);
            return ApiResponse.success("删除成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
}
