package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attribute_value")
public class AttributeValue {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private String value;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
