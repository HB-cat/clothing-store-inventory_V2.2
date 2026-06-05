package com.heben.clothing_s_i_server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heben.clothing_s_i_server.entity.AttributeGroup;
import com.heben.clothing_s_i_server.entity.AttributeValue;
import com.heben.clothing_s_i_server.mapper.AttributeGroupMapper;
import com.heben.clothing_s_i_server.mapper.AttributeValueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttributeService {

    private final AttributeGroupMapper attributeGroupMapper;
    private final AttributeValueMapper attributeValueMapper;

    /**
     * 获取属性组列表（每个组含 values 列表）
     */
    public List<GroupVO> getGroups(Long userId) {
        List<AttributeGroup> groups = attributeGroupMapper.selectList(
                new LambdaQueryWrapper<AttributeGroup>()
                        .eq(AttributeGroup::getUserId, userId)
                        .orderByAsc(AttributeGroup::getId)
        );

        List<AttributeValue> allValues = attributeValueMapper.selectList(null);
        Map<Long, List<AttributeValue>> valueMap = allValues.stream()
                .collect(Collectors.groupingBy(AttributeValue::getGroupId));

        return groups.stream().map(g -> {
            GroupVO vo = new GroupVO();
            vo.setId(g.getId());
            vo.setUserId(g.getUserId());
            vo.setName(g.getName());
            vo.setIsPreset(g.getIsPreset());
            vo.setValues(valueMap.getOrDefault(g.getId(), List.of()));
            vo.setCreatedAt(g.getCreatedAt());
            vo.setUpdatedAt(g.getUpdatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 新增属性组
     */
    public AttributeGroup addGroup(Long userId, String name) {
        AttributeGroup group = new AttributeGroup();
        group.setUserId(userId);
        group.setName(name);
        group.setIsPreset(0);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        attributeGroupMapper.insert(group);
        return group;
    }

    /**
     * 删除属性组（级联删除属性值）
     */
    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        AttributeGroup group = attributeGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new IllegalArgumentException("属性组不存在");
        }
        if (group.getIsPreset() == 1) {
            throw new IllegalArgumentException("预置属性组不可删除，以确保新用户能快速上手");
        }
        // 级联删除属性值
        attributeValueMapper.delete(
                new LambdaQueryWrapper<AttributeValue>().eq(AttributeValue::getGroupId, groupId)
        );
        attributeGroupMapper.deleteById(groupId);
    }

    /**
     * 批量添加属性值
     */
    @Transactional
    public List<AttributeValue> addValues(Long userId, Long groupId, List<String> values) {
        AttributeGroup group = attributeGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new IllegalArgumentException("属性组不存在");
        }

        // 获取当前最大 sort_order
        AttributeValue maxVal = attributeValueMapper.selectOne(
                new LambdaQueryWrapper<AttributeValue>()
                        .eq(AttributeValue::getGroupId, groupId)
                        .orderByDesc(AttributeValue::getSortOrder)
                        .last("LIMIT 1")
        );
        int nextOrder = (maxVal == null) ? 1 : maxVal.getSortOrder() + 1;

        List<AttributeValue> result = new java.util.ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            AttributeValue val = new AttributeValue();
            val.setGroupId(groupId);
            val.setValue(values.get(i));
            val.setSortOrder(nextOrder + i);
            val.setCreatedAt(LocalDateTime.now());
            attributeValueMapper.insert(val);
            result.add(val);
        }
        return result;
    }

    /**
     * 删除属性值
     */
    public void deleteValue(Long userId, Long valueId) {
        AttributeValue value = attributeValueMapper.selectById(valueId);
        if (value == null) {
            throw new IllegalArgumentException("属性值不存在");
        }
        AttributeGroup group = attributeGroupMapper.selectById(value.getGroupId());
        if (group == null || !group.getUserId().equals(userId)) {
            throw new IllegalArgumentException("属性值不存在");
        }
        if (group.getIsPreset() == 1) {
            throw new IllegalArgumentException("预置属性组的属性值不可删除");
        }
        attributeValueMapper.deleteById(valueId);
    }

    // ========== 内部类 ==========

    @lombok.Data
    public static class GroupVO {
        private Long id;
        private Long userId;
        private String name;
        private Integer isPreset;
        private List<AttributeValue> values;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
