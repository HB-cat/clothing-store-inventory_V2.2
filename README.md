# Clothing-SI V2.2

面向小商户的全栈进销存协同系统，覆盖零售商、批发商、买家三种角色 + 管理员后台。

## 系统架构

```
后端: Spring Boot 3.2.0 + MyBatis-Plus + MySQL 8.0
客户端: Android (Gradle, 待开发)
管理后台: Thymeleaf + Bootstrap 5
```

## 项目结构

```
Clothing_S_I_V2.2/
├── clothing_SI_server/          # 后端 Maven 项目
│   ├── src/main/java/           # Java 源码
│   ├── src/main/resources/      # 配置文件 + Thymeleaf 模板
│   ├── pom.xml                  # Maven 依赖
│   └── test-*.http              # API 测试文件 (IDEA HTTP Client)
├── Clothing_SI_client/          # Android 客户端 (待开发)
├── Clothing_SI_database/        # 数据库脚本
│   ├── init.sql                 # 建表 (19张表)
│   └── seed_data.sql            # 种子数据
└── *.md                         # 项目文档
```

## 已实现功能

| Phase | 内容 | 状态 | 接口数 |
|:-----:|------|:----:|:------:|
| 2.1 | 注册登录 | ✅ | 3 |
| 2.2 | 商品体系 (分类/属性/商品/共享池) | ✅ | 17 |
| 2.3 | 记账+进货+账单+退货 | ✅ | 17 |
| 2.4 | 聊天+预约 | ✅ | 11 |
| 2.5 | 经营分析 | ✅ | 7 |
| 2.6 | 智能体 (DeepSeek API 转发) | ✅ | 1 |
| 2.7 | 管理员后台 (Thymeleaf 页面) | ✅ | 4页面 |

**总计: 45/55 个 API 接口 + 4 个管理页面**

## 快速启动

### 1. 数据库

在 HeidiSQL 或 MySQL 客户端中执行:

```sql
CREATE DATABASE IF NOT EXISTS clothing_si_db;
```

然后运行 `Clothing_SI_database/init.sql` 建表。

### 2. 配置

编辑 `clothing_SI_server/src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=root
# 按需修改数据库密码
```

### 3. 启动后端

```bash
cd clothing_SI_server
mvn spring-boot:run
```

### 4. 访问

- API: `http://localhost:8080/api/...`
- 管理后台: `http://localhost:8080/admin/login`
- 管理员账号: `heben` / `heben0831`

### 5. 智能体配置

编辑 `application.properties`:

```properties
deepseek.api.key=sk-你的DeepSeek API Key
```

## 核心设计

| 要点 | 说明 |
|------|------|
| Token认证 | UUID, 30天有效期, 存数据库, 每次请求查库校验 |
| 数据隔离 | 所有表含 user_id, Service 层显式 WHERE user_id = ? |
| 属性组合 | product_attr_combination 表, attrs_json 存 JSON |
| 乐观锁扣库存 | version 字段, UPDATE ... WHERE version = ? |
| 响应格式 | 统一 ApiResponse {code, message, data} |

## 技术栈

| 组件 | 版本 |
|------|:----:|
| JDK | 17 |
| Spring Boot | 3.2.0 |
| MyBatis-Plus | 3.5.7 |
| MySQL | 8.0 |
| Thymeleaf | 3.1.2 |
| Lombok | 1.18.30 |
| Bootstrap | 5.3.3 |

---

*Clothing-SI V2.2 — HeBen_Cat*
