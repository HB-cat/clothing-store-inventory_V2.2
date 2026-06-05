-- ============================================================
-- Clothing-SI V2.2 - 数据库初始化脚本（仅建表）
-- 说明：请先在 HeidiSQL 中手动创建 clothing_si_db 数据库
--       然后选中该数据库，再执行此文件
-- ============================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '系统生成的唯一ID码',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt哈希存储',
    role TINYINT NOT NULL COMMENT '角色：1=零售商, 2=批发商, 3=买家, 4=管理员',
    shop_name VARCHAR(50) DEFAULT NULL COMMENT '店铺名称',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    token VARCHAR(64) DEFAULT NULL COMMENT '当前登录Token',
    token_expire DATETIME DEFAULT NULL COMMENT 'Token过期时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1=正常, 0=禁用',
    appointment_ban_until DATETIME DEFAULT NULL COMMENT '预约功能封禁截止时间',
    appointment_ban_count INT DEFAULT 0 COMMENT '累计毁约次数',
    created_at DATETIME NOT NULL COMMENT '注册时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 买家个人信息表
CREATE TABLE IF NOT EXISTS buyer_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '买家用户ID',
    age INT DEFAULT NULL COMMENT '年龄',
    height INT DEFAULT NULL COMMENT '身高(cm)',
    weight DECIMAL(5,1) DEFAULT NULL COMMENT '体重(kg)',
    bust DECIMAL(5,1) DEFAULT NULL COMMENT '胸围',
    waist DECIMAL(5,1) DEFAULT NULL COMMENT '腰围',
    hip DECIMAL(5,1) DEFAULT NULL COMMENT '臀围',
    style_preference VARCHAR(255) DEFAULT NULL COMMENT '穿衣风格偏好',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买家个人信息表';

-- 3. 商品分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    is_preset TINYINT DEFAULT 0 COMMENT '是否预置分类',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_user_sort (user_id, sort_order),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 4. 属性组表
CREATE TABLE IF NOT EXISTS attribute_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '属性组ID',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    name VARCHAR(50) NOT NULL COMMENT '属性组名称',
    is_preset TINYINT DEFAULT 0 COMMENT '是否预置属性组',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='属性组表';

-- 5. 属性值表
CREATE TABLE IF NOT EXISTS attribute_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '属性值ID',
    group_id BIGINT NOT NULL COMMENT '所属属性组',
    value VARCHAR(50) NOT NULL COMMENT '属性值',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    created_at DATETIME NOT NULL,
    INDEX idx_group (group_id),
    FOREIGN KEY (group_id) REFERENCES attribute_group(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='属性值表';

-- 6. 商品主表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '所属分类',
    purchase_price DECIMAL(10,2) NOT NULL COMMENT '进价',
    selling_price DECIMAL(10,2) NOT NULL COMMENT '售价',
    image_url VARCHAR(255) DEFAULT NULL COMMENT '商品图片URL',
    has_attributes TINYINT DEFAULT 0 COMMENT '是否有属性组合',
    source TINYINT DEFAULT 1 COMMENT '来源：1=自主创建, 2=从批发商导入',
    source_tx_id BIGINT DEFAULT NULL COMMENT '来源交易ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '软删除标记',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_user (user_id),
    INDEX idx_category (category_id),
    INDEX idx_user_name (user_id, name),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品主表';

-- 7. 商品属性组合库存表
CREATE TABLE IF NOT EXISTS product_attr_combination (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '所属商品',
    attrs_json VARCHAR(500) NOT NULL COMMENT '属性组合JSON',
    stock INT DEFAULT 0 COMMENT '当前库存数量',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_product (product_id),
    FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品属性组合库存表';

-- 8. 草稿账单表
CREATE TABLE IF NOT EXISTS draft_bill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '所属用户',
    bill_index TINYINT NOT NULL COMMENT '账单序号（1-6）',
    total_amount DECIMAL(10,2) DEFAULT 0 COMMENT '总金额',
    item_count INT DEFAULT 0 COMMENT '商品种类数',
    status TINYINT DEFAULT 0 COMMENT '状态：0=编辑中, 1=已提交',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_user_status (user_id, status),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='草稿账单表';

-- 9. 草稿账单明细表
CREATE TABLE IF NOT EXISTS draft_bill_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL COMMENT '所属草稿账单',
    product_id BIGINT NOT NULL COMMENT '商品',
    combination_id BIGINT DEFAULT NULL COMMENT '属性组合',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    created_at DATETIME NOT NULL,
    INDEX idx_bill (bill_id),
    FOREIGN KEY (bill_id) REFERENCES draft_bill(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (combination_id) REFERENCES product_attr_combination(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='草稿账单明细表';

-- 10. 销售记录主表
CREATE TABLE IF NOT EXISTS sale_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '卖家用户ID',
    buyer_id BIGINT DEFAULT NULL COMMENT '关联买家ID',
    buyer_name VARCHAR(50) DEFAULT '散客' COMMENT '买家名称冗余',
    source TINYINT DEFAULT 1 COMMENT '来源：1=快捷指令, 2=新建账单提交',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    total_profit DECIMAL(10,2) NOT NULL COMMENT '总毛利',
    item_count INT NOT NULL COMMENT '商品种类数',
    sale_time DATETIME NOT NULL COMMENT '销售时间',
    created_at DATETIME NOT NULL,
    INDEX idx_user_time (user_id, sale_time),
    INDEX idx_buyer (buyer_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (buyer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售记录主表';

-- 11. 销售记录明细表
CREATE TABLE IF NOT EXISTS sale_record_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT NOT NULL COMMENT '所属销售记录',
    product_id BIGINT NOT NULL COMMENT '商品',
    combination_id BIGINT DEFAULT NULL COMMENT '属性组合',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称冗余',
    attrs_desc VARCHAR(200) DEFAULT NULL COMMENT '属性描述冗余',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '售价',
    purchase_price DECIMAL(10,2) NOT NULL COMMENT '进价',
    is_returned TINYINT DEFAULT 0 COMMENT '是否已退货',
    returned_at DATETIME DEFAULT NULL COMMENT '退货时间',
    created_at DATETIME NOT NULL,
    INDEX idx_sale (sale_id),
    INDEX idx_product_return (product_id, is_returned),
    FOREIGN KEY (sale_id) REFERENCES sale_record(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (combination_id) REFERENCES product_attr_combination(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售记录明细表';

-- 12. 进货记录主表
CREATE TABLE IF NOT EXISTS purchase_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '进货方用户ID',
    source TINYINT NOT NULL COMMENT '来源：1=我的商品,2=新建并入库,3=批发商共享库',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    supplier_name VARCHAR(100) DEFAULT NULL COMMENT '供应商名称',
    created_at DATETIME NOT NULL,
    INDEX idx_user (user_id),
    INDEX idx_source (source),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货记录主表';

-- 13. 进货记录明细表
CREATE TABLE IF NOT EXISTS purchase_record_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id BIGINT NOT NULL COMMENT '所属进货记录',
    product_id BIGINT NOT NULL COMMENT '商品',
    combination_id BIGINT DEFAULT NULL COMMENT '属性组合',
    quantity INT NOT NULL COMMENT '进货数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '进价',
    created_at DATETIME NOT NULL,
    INDEX idx_purchase (purchase_id),
    FOREIGN KEY (purchase_id) REFERENCES purchase_record(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (combination_id) REFERENCES product_attr_combination(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货记录明细表';

-- 14. 批零交易记录表
CREATE TABLE IF NOT EXISTS wholesale_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wholesaler_id BIGINT NOT NULL COMMENT '批发商ID',
    retailer_id BIGINT NOT NULL COMMENT '零售商ID',
    product_id BIGINT NOT NULL COMMENT '商品',
    combination_id BIGINT DEFAULT NULL COMMENT '属性组合',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '交易单价',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status TINYINT DEFAULT 0 COMMENT '状态：0=待确认,1=已确认,2=已拒绝,3=已取消',
    created_at DATETIME NOT NULL,
    confirmed_at DATETIME DEFAULT NULL COMMENT '确认时间',
    INDEX idx_wholesaler (wholesaler_id),
    INDEX idx_retailer (retailer_id),
    INDEX idx_status (status),
    FOREIGN KEY (wholesaler_id) REFERENCES sys_user(id),
    FOREIGN KEY (retailer_id) REFERENCES sys_user(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (combination_id) REFERENCES product_attr_combination(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批零交易记录表';

-- 15. 预约表
CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    seller_id BIGINT NOT NULL COMMENT '卖家ID',
    appointment_date DATE NOT NULL COMMENT '预约日期',
    time_slot VARCHAR(20) NOT NULL COMMENT '时段',
    status TINYINT DEFAULT 0 COMMENT '状态：0=待确认,1=已确认,2=已完成,3=已拒绝,4=已取消,5=毁约',
    reject_reason VARCHAR(200) DEFAULT NULL COMMENT '拒绝理由',
    reminder_sent TINYINT DEFAULT 0 COMMENT '到店提醒是否已发送',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller_status (seller_id, status),
    INDEX idx_date (appointment_date),
    FOREIGN KEY (buyer_id) REFERENCES sys_user(id),
    FOREIGN KEY (seller_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- 16. 聊天会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    participant_a BIGINT NOT NULL COMMENT '参与方A',
    participant_b BIGINT NOT NULL COMMENT '参与方B',
    last_message TEXT DEFAULT NULL COMMENT '最后一条消息摘要',
    last_message_at DATETIME DEFAULT NULL COMMENT '最后消息时间',
    unread_a INT DEFAULT 0 COMMENT 'A的未读数',
    unread_b INT DEFAULT 0 COMMENT 'B的未读数',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_participants (participant_a, participant_b),
    FOREIGN KEY (participant_a) REFERENCES sys_user(id),
    FOREIGN KEY (participant_b) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 17. 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '所属会话',
    sender_id BIGINT NOT NULL COMMENT '发送方',
    message_type TINYINT NOT NULL COMMENT '类型：1=文本,2=图片,3=商品卡片,4=商品单推荐',
    content TEXT DEFAULT NULL COMMENT '文本内容',
    ref_id BIGINT DEFAULT NULL COMMENT '引用ID',
    created_at DATETIME NOT NULL,
    INDEX idx_session (session_id),
    INDEX idx_sender (sender_id),
    FOREIGN KEY (session_id) REFERENCES chat_session(id),
    FOREIGN KEY (sender_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 18. 批发商共享商品池表
CREATE TABLE IF NOT EXISTS shared_product_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wholesaler_id BIGINT NOT NULL COMMENT '批发商ID',
    product_id BIGINT NOT NULL COMMENT '共享的商品',
    shared_price DECIMAL(10,2) NOT NULL COMMENT '预设共享价',
    is_active TINYINT DEFAULT 1 COMMENT '是否仍在共享中',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_wholesaler (wholesaler_id),
    INDEX idx_product (product_id),
    FOREIGN KEY (wholesaler_id) REFERENCES sys_user(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批发商共享商品池表';

-- 19. 通知设置表
CREATE TABLE IF NOT EXISTS notification_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户',
    agent_suggestion TINYINT DEFAULT 1 COMMENT '智能体主动建议',
    appointment_request TINYINT DEFAULT 1 COMMENT '预约请求通知',
    appointment_result TINYINT DEFAULT 1 COMMENT '预约结果通知',
    arrival_reminder TINYINT DEFAULT 1 COMMENT '到店提醒',
    chat_message TINYINT DEFAULT 1 COMMENT '新聊天消息',
    wholesale_request TINYINT DEFAULT 1 COMMENT '批零进货请求',
    wholesale_confirm TINYINT DEFAULT 1 COMMENT '批零进货确认',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知设置表';

-- ============================================================
-- 建表完成
-- ============================================================
