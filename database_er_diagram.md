# Clothing-SI V2.2 数据库ER图

> 版本：V2.2 | 日期：2026-06-04 | 对应PRD：功能梳理确认版

---

## 一、表清单（共20张表）

| 编号 | 表名 | 说明 | 所属模块 |
|:---:|------|------|---------|
| 1 | `sys_user` | 用户表（零售商/批发商/买家/管理员） | 注册登录 |
| 2 | `buyer_info` | 买家个人信息（年龄/身高/体重/三围/偏好） | 买家端 |
| 3 | `category` | 商品分类 | 商品体系 |
| 4 | `attribute_group` | 属性组（颜色/尺码/风格/面料/季节） | 商品体系 |
| 5 | `attribute_value` | 属性值（红色/M/休闲/棉/春） | 商品体系 |
| 6 | `product` | 商品主表 | 商品体系 |
| 7 | `product_attr_combination` | 商品属性组合库存表 | 商品体系 |
| 8 | `draft_bill` | 草稿账单 | 记账 |
| 9 | `draft_bill_item` | 草稿账单明细 | 记账 |
| 10 | `sale_record` | 销售记录主表 | 记账 |
| 11 | `sale_record_item` | 销售记录明细 | 记账 |
| 12 | `purchase_record` | 进货记录主表 | 进货 |
| 13 | `purchase_record_item` | 进货记录明细 | 进货 |
| 14 | `wholesale_transaction` | 批零交易记录 | 批零交易 |
| 15 | `appointment` | 预约表 | 预约管理 |
| 16 | `chat_session` | 聊天会话 | 聊天 |
| 17 | `chat_message` | 聊天消息 | 聊天 |
| 18 | `shared_product_pool` | 批发商共享商品池 | 商品共享 |
| 19 | `habit_log` | 智能体习惯学习日志 | 智能体 |
| 20 | `notification_setting` | 通知设置 | 系统设置 |

---

## 二、各表字段详情

### 2.1 `sys_user` — 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(20) | UNIQUE, NOT NULL | 系统生成的唯一ID码，格式 `角色_5位随机`，如 `ret_Ab3Xy` |
| password | VARCHAR(255) | NOT NULL | BCrypt哈希存储 |
| role | TINYINT | NOT NULL | 角色：1=零售商, 2=批发商, 3=买家, 4=管理员 |
| shop_name | VARCHAR(50) | DEFAULT NULL | 店铺名称（零售商/批发商必填） |
| nickname | VARCHAR(50) | DEFAULT NULL | 昵称（买家必填） |
| avatar_url | VARCHAR(255) | DEFAULT NULL | 头像URL |
| token | VARCHAR(64) | DEFAULT NULL | 当前登录Token |
| token_expire | DATETIME | DEFAULT NULL | Token过期时间 |
| status | TINYINT | DEFAULT 1 | 状态：1=正常, 0=禁用 |
| appointment_ban_until | DATETIME | DEFAULT NULL | 预约功能封禁截止时间（买家毁约6次后设置） |
| appointment_ban_count | INT | DEFAULT 0 | 累计毁约次数 |
| created_at | DATETIME | NOT NULL | 注册时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引**：`idx_username`(username), `idx_role`(role)

### 2.2 `buyer_info` — 买家个人信息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK→sys_user.id, UNIQUE | 买家用户ID |
| age | INT | DEFAULT NULL | 年龄 |
| height | INT | DEFAULT NULL | 身高(cm) |
| weight | DECIMAL(5,1) | DEFAULT NULL | 体重(kg) |
| bust | DECIMAL(5,1) | DEFAULT NULL | 胸围 |
| waist | DECIMAL(5,1) | DEFAULT NULL | 腰围 |
| hip | DECIMAL(5,1) | DEFAULT NULL | 臀围 |
| style_preference | VARCHAR(255) | DEFAULT NULL | 穿衣风格偏好，多个用逗号分隔 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

### 2.3 `category` — 商品分类表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 分类ID |
| user_id | BIGINT | FK→sys_user.id | 所属用户 |
| name | VARCHAR(50) | NOT NULL | 分类名称（如"连衣裙类"） |
| sort_order | INT | DEFAULT 0 | 排序序号，拖拽后持久化 |
| is_preset | TINYINT | DEFAULT 0 | 是否预置分类 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**索引**：`idx_user_sort`(user_id, sort_order)
**预置数据**：连衣裙类、T恤类、衬衫类、裤子类、外套类（每个新卖家注册时自动插入）

### 2.4 `attribute_group` — 属性组表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 属性组ID |
| user_id | BIGINT | FK→sys_user.id | 所属用户 |
| name | VARCHAR(50) | NOT NULL | 属性组名称（如"颜色""尺码"） |
| is_preset | TINYINT | DEFAULT 0 | 是否预置属性组 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**预置数据**：风格、面料、颜色、尺码、季节（每个新卖家注册时自动插入）

### 2.5 `attribute_value` — 属性值表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 属性值ID |
| group_id | BIGINT | FK→attribute_group.id | 所属属性组 |
| value | VARCHAR(50) | NOT NULL | 属性值（如"红色""M"） |
| sort_order | INT | DEFAULT 0 | 排序序号 |
| created_at | DATETIME | NOT NULL | |

**索引**：`idx_group`(group_id)

### 2.6 `product` — 商品主表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 商品ID |
| user_id | BIGINT | FK→sys_user.id | 所属用户（零售商/批发商） |
| name | VARCHAR(100) | NOT NULL | 商品名称 |
| category_id | BIGINT | FK→category.id | 所属分类 |
| purchase_price | DECIMAL(10,2) | NOT NULL | 进价 |
| selling_price | DECIMAL(10,2) | NOT NULL | 售价 |
| image_url | VARCHAR(255) | DEFAULT NULL | 商品图片URL |
| has_attributes | TINYINT | DEFAULT 0 | 是否有属性组合：0=无属性, 1=有属性 |
| source | TINYINT | DEFAULT 1 | 来源：1=自主创建, 2=从批发商导入 |
| source_tx_id | BIGINT | DEFAULT NULL | 来源交易ID（批零交易时关联wholesale_transaction.id） |
| is_deleted | TINYINT | DEFAULT 0 | 软删除标记 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**索引**：`idx_user`(user_id), `idx_category`(category_id), `idx_user_name`(user_id, name)

### 2.7 `product_attr_combination` — 商品属性组合库存表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| product_id | BIGINT | FK→product.id | 所属商品 |
| attrs_json | VARCHAR(500) | NOT NULL | 属性组合JSON，如 `{"颜色":"红色","尺码":"M"}` |
| stock | INT | DEFAULT 0 | 当前库存数量 |
| version | INT | DEFAULT 0 | 乐观锁版本号（用于并发控制） |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**索引**：`idx_product`(product_id)
**说明**：一个商品有多少个属性组合，这里就有多少条记录。无属性商品只生成1条记录（attrs_json为`{}`）。

### 2.8 `draft_bill` — 草稿账单表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK→sys_user.id | 所属用户 |
| bill_index | TINYINT | NOT NULL | 账单序号（1-6，最多6个草稿） |
| total_amount | DECIMAL(10,2) | DEFAULT 0 | 总金额（实时计算） |
| item_count | INT | DEFAULT 0 | 商品种类数 |
| status | TINYINT | DEFAULT 0 | 状态：0=编辑中, 1=已提交 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**索引**：`idx_user_status`(user_id, status)

### 2.9 `draft_bill_item` — 草稿账单明细表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| bill_id | BIGINT | FK→draft_bill.id | 所属草稿账单 |
| product_id | BIGINT | FK→product.id | 商品 |
| combination_id | BIGINT | FK→product_attr_combination.id | 属性组合（可选，无属性商品为NULL） |
| quantity | INT | NOT NULL | 数量 |
| unit_price | DECIMAL(10,2) | NOT NULL | 单价 |
| created_at | DATETIME | NOT NULL | |

**索引**：`idx_bill`(bill_id)
**约束**：每个账单最多30条明细

### 2.10 `sale_record` — 销售记录主表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK→sys_user.id | 卖家用户ID |
| buyer_id | BIGINT | FK→sys_user.id | 关联买家ID（必填，散客为特殊值-1或NULL标记） |
| buyer_name | VARCHAR(50) | DEFAULT '散客' | 买家名称冗余（方便显示） |
| source | TINYINT | DEFAULT 1 | 来源：1=快捷指令, 2=新建账单提交 |
| total_amount | DECIMAL(10,2) | NOT NULL | 总金额 |
| total_profit | DECIMAL(10,2) | NOT NULL | 总毛利 |
| item_count | INT | NOT NULL | 商品种类数 |
| sale_time | DATETIME | NOT NULL | 销售时间 |
| created_at | DATETIME | NOT NULL | |

**索引**：`idx_user_time`(user_id, sale_time), `idx_buyer`(buyer_id)

### 2.11 `sale_record_item` — 销售记录明细表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| sale_id | BIGINT | FK→sale_record.id | 所属销售记录 |
| product_id | BIGINT | FK→product.id | 商品 |
| combination_id | BIGINT | FK→product_attr_combination.id | 属性组合（可选） |
| product_name | VARCHAR(100) | NOT NULL | 商品名称冗余（退货时即使商品被删也能显示） |
| attrs_desc | VARCHAR(200) | DEFAULT NULL | 属性描述冗余（如"红色-M码"） |
| quantity | INT | NOT NULL | 数量 |
| unit_price | DECIMAL(10,2) | NOT NULL | 售价 |
| purchase_price | DECIMAL(10,2) | NOT NULL | 进价（用于毛利计算，记录当时价格） |
| is_returned | TINYINT | DEFAULT 0 | 是否已退货 |
| returned_at | DATETIME | DEFAULT NULL | 退货时间 |
| created_at | DATETIME | NOT NULL | |

**索引**：`idx_sale`(sale_id), `idx_product_return`(product_id, is_returned)

### 2.12 `purchase_record` — 进货记录主表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK→sys_user.id | 进货方用户ID |
| source | TINYINT | NOT NULL | 来源：1=我的商品, 2=新建并入库, 3=批发商共享库 |
| total_amount | DECIMAL(10,2) | NOT NULL | 总金额 |
| supplier_name | VARCHAR(100) | DEFAULT NULL | 供应商名称（来源3时为批发商店铺名） |
| created_at | DATETIME | NOT NULL | |

**索引**：`idx_user`(user_id), `idx_source`(source)

### 2.13 `purchase_record_item` — 进货记录明细表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| purchase_id | BIGINT | FK→purchase_record.id | 所属进货记录 |
| product_id | BIGINT | FK→product.id | 商品 |
| combination_id | BIGINT | FK→product_attr_combination.id | 属性组合 |
| quantity | INT | NOT NULL | 进货数量 |
| unit_price | DECIMAL(10,2) | NOT NULL | 进价 |
| created_at | DATETIME | NOT NULL | |

### 2.14 `wholesale_transaction` — 批零交易记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| wholesaler_id | BIGINT | FK→sys_user.id | 批发商ID |
| retailer_id | BIGINT | FK→sys_user.id | 零售商ID |
| product_id | BIGINT | FK→product.id | 商品 |
| combination_id | BIGINT | FK→product_attr_combination.id | 属性组合 |
| quantity | INT | NOT NULL | 数量 |
| unit_price | DECIMAL(10,2) | NOT NULL | 交易单价（共享价） |
| total_amount | DECIMAL(10,2) | NOT NULL | 总金额 |
| status | TINYINT | DEFAULT 0 | 状态：0=待确认, 1=已确认, 2=已拒绝, 3=已取消 |
| created_at | DATETIME | NOT NULL | |
| confirmed_at | DATETIME | DEFAULT NULL | 确认时间 |

**索引**：`idx_wholesaler`(wholesaler_id), `idx_retailer`(retailer_id), `idx_status`(status)

### 2.15 `appointment` — 预约表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| buyer_id | BIGINT | FK→sys_user.id | 买家ID |
| seller_id | BIGINT | FK→sys_user.id | 卖家ID |
| appointment_date | DATE | NOT NULL | 预约日期 |
| time_slot | VARCHAR(20) | NOT NULL | 时段，如 "14:00-16:00" |
| status | TINYINT | DEFAULT 0 | 状态：0=待确认, 1=已确认, 2=已完成, 3=已拒绝, 4=已取消, 5=毁约 |
| reject_reason | VARCHAR(200) | DEFAULT NULL | 拒绝理由（卖家拒绝时必填） |
| reminder_sent | TINYINT | DEFAULT 0 | 到店提醒是否已发送 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**索引**：`idx_buyer`(buyer_id), `idx_seller_status`(seller_id, status), `idx_date`(appointment_date)

### 2.16 `chat_session` — 聊天会话表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| participant_a | BIGINT | FK→sys_user.id | 参与方A |
| participant_b | BIGINT | FK→sys_user.id | 参与方B |
| last_message | TEXT | DEFAULT NULL | 最后一条消息摘要 |
| last_message_at | DATETIME | DEFAULT NULL | 最后消息时间 |
| unread_a | INT | DEFAULT 0 | A的未读数 |
| unread_b | INT | DEFAULT 0 | B的未读数 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**索引**：`idx_participants`(participant_a, participant_b, UNIQUE)

### 2.17 `chat_message` — 聊天消息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| session_id | BIGINT | FK→chat_session.id | 所属会话 |
| sender_id | BIGINT | FK→sys_user.id | 发送方 |
| message_type | TINYINT | NOT NULL | 类型：1=文本, 2=图片, 3=商品卡片, 4=商品单推荐 |
| content | TEXT | DEFAULT NULL | 文本内容 |
| ref_id | BIGINT | DEFAULT NULL | 引用ID（商品卡片→product.id, 商品单推荐→共享商品ID列表JSON） |
| created_at | DATETIME | NOT NULL | |

**索引**：`idx_session`(session_id), `idx_sender`(sender_id)

### 2.18 `shared_product_pool` — 批发商共享商品池表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| wholesaler_id | BIGINT | FK→sys_user.id | 批发商ID |
| product_id | BIGINT | FK→product.id | 共享的商品（批发商的商品） |
| shared_price | DECIMAL(10,2) | NOT NULL | 预设共享价 |
| is_active | TINYINT | DEFAULT 1 | 是否仍在共享中 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**索引**：`idx_wholesaler`(wholesaler_id), `idx_product`(product_id)

### 2.19 `habit_log` — 智能体习惯学习日志表（本地Room）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NOT NULL | 用户ID |
| action_type | VARCHAR(30) | NOT NULL | 操作类型：SELL, QUERY_SALE, OPEN_PAGE, PURCHASE, ADD_BILL 等 |
| action_time | TIMESTAMP | NOT NULL | 操作时间 |
| extra_info | TEXT | DEFAULT NULL | JSON扩展信息（商品名、页面名等） |
| synced | TINYINT | DEFAULT 0 | 是否已同步云端 |

**索引**：`idx_user_action`(user_id, action_type), `idx_time`(action_time)
**说明**：此表主要存在客户端Room，云端可选同步做备份分析。

### 2.20 `notification_setting` — 通知设置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK→sys_user.id, UNIQUE | 用户 |
| agent_suggestion | TINYINT | DEFAULT 1 | 智能体主动建议 |
| appointment_request | TINYINT | DEFAULT 1 | 预约请求通知 |
| appointment_result | TINYINT | DEFAULT 1 | 预约结果通知 |
| arrival_reminder | TINYINT | DEFAULT 1 | 到店提醒 |
| chat_message | TINYINT | DEFAULT 1 | 新聊天消息 |
| wholesale_request | TINYINT | DEFAULT 1 | 批零进货请求 |
| wholesale_confirm | TINYINT | DEFAULT 1 | 批零进货确认 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

---

## 三、表关系总图

```
sys_user (1) ──< (n) product                 # 一个用户有多个商品
sys_user (1) ──< (n) category                # 一个用户有多个分类
sys_user (1) ──< (n) attribute_group         # 一个用户有多个属性组
sys_user (1) ──< (n) sale_record             # 一个用户有多个销售记录
sys_user (1) ──< (n) draft_bill              # 一个用户有多个草稿账单
sys_user (1) ──< (n) purchase_record         # 一个用户有多个进货记录
sys_user (1) ──< (n) appointment (as seller) # 一个卖家有多个预约
sys_user (1) ──< (n) appointment (as buyer)  # 一个买家有多个预约

product (1) ──< (n) product_attr_combination # 一个商品有多个属性组合
product (1) ──> (1) category                 # 一个商品属于一个分类

attribute_group (1) ──< (n) attribute_value  # 一个属性组有多个属性值

sale_record (1) ──< (n) sale_record_item     # 一笔销售有多条明细
draft_bill (1) ──< (n) draft_bill_item       # 一个草稿账单有多条明细
purchase_record (1) ──< (n) purchase_record_item # 一笔进货有多条明细

sale_record_item (n) ──> (1) product         # 明细关联商品
sale_record_item (n) ──> (1) product_attr_combination # 明细关联属性组合

wholesale_transaction (n) ──> (1) sys_user (wholesaler) # 批零交易关联批发商
wholesale_transaction (n) ──> (1) sys_user (retailer)   # 批零交易关联零售商

chat_session (1) ──< (n) chat_message        # 一个会话有多条消息
chat_session ──> sys_user (participant_a)    # 会话参与方A
chat_session ──> sys_user (participant_b)    # 会话参与方B

sys_user (1) ──> (1) buyer_info              # 一个买家有一条个人信息
sys_user (1) ──> (1) notification_setting    # 一个用户有一份通知设置
```

---

## 四、建表SQL要点

### 核心约束

```sql
-- 1. 用户名唯一且格式校验（应用层）
ALTER TABLE sys_user ADD CONSTRAINT uk_username UNIQUE (username);

-- 2. 每个用户最多6个草稿账单（应用层校验）
-- 3. 每个账单最多30条明细（应用层校验）

-- 4. 商品删除校验（应用层）：有库存或未完成订单时禁止删除
-- 5. 分类删除校验（应用层）：有商品时提示转移或批量删除

-- 6. 预约状态枚举在应用层定义
-- 7. 批零交易状态在应用层定义

-- 8. 库存扣减使用乐观锁（version字段），避免并发超卖
UPDATE product_attr_combination 
SET stock = stock - ?, version = version + 1 
WHERE id = ? AND version = ? AND stock >= ?;
```

### 预置数据插入时机

新卖家（零售商/批发商）注册成功后，自动插入：
1. 5个预置分类
2. 5个预置属性组 + 各属性组的默认属性值
3. 1条默认通知设置（全部开启）

---

## 五、ER图说明

```
符号约定：
  (1) ──< (n)  ：一对多关系（1端为主表，n端为从表）
  (1) ──> (1)  ：一对一关系
  ──>          ：外键引用方向
  PK           ：主键
  FK           ：外键
```
