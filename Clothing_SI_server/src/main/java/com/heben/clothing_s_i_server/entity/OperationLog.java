package com.heben.clothing_s_i_server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String operationId;

    private Long userId;

    private String operationType;

    private Integer status; // 1=成功

    private LocalDateTime createdAt;
}
