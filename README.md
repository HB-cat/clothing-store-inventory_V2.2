# Clothing-SI V2.2

面向小商户的全栈进销存协同系统，覆盖零售商、批发商、买家三种角色 + 管理员后台。

## 系统架构

```
后端: Spring Boot 3.2.0 + MyBatis-Plus + MySQL 8.0
客户端: Android (Java + Retrofit2 + Material Design)
管理后台: Thymeleaf + Bootstrap 5
```

## 项目结构

```
Clothing_S_I_V2.2/
├── clothing_SI_server/          # 后端 Maven 项目
│   ├── src/main/java/           # Java 源码 (Controller/Service/Entity)
│   ├── src/main/resources/      # 配置文件 + Thymeleaf 模板
│   ├── pom.xml                  # Maven 依赖
│   └── test-*.http              # API 测试文件
├── Clothing_SI_client/          # Android 客户端
│   └── app/src/main/
│       ├── java/.../activity/   # 25个 Activity + 4个 Fragment
│       ├── java/.../adapter/    # 8个 RecyclerView Adapter
│       ├── java/.../model/      # 全部数据模型
│       ├── java/.../util/       # SoundManager/TokenManager/AppContext/BottomNav
│       ├── java/.../network/    # ApiClient + ApiService (46+接口)
│       └── res/layout/          # 50个布局文件（含ui_preview全面改造）
├── ui_preview/                  # 17个HTML设计稿（暖橙主题标准）
├── Clothing_SI_database/        # 数据库脚本
│   ├── init.sql                 # 建表 (19张表)
│   └── seed_data.sql            # 种子数据
├── 工作进度表.md                # 详细开发进度与修复记录
├── 接受引导书.md                # 项目验收/测试引导
└── README.md                    # 本文件
```

## 完成状态

| 模块 | 状态 | 详情 |
|:----:|:----:|------|
| 后端 API (45+接口) | ✅ 100% | 注册登录/商品/销售/进货/分析/聊天/预约/智能体 |
| 管理员后台 (4页面) | ✅ 100% | Thymeleaf 页面，管理员账号 heben/heben0831 |
| Android 客户端 | ✅ 100% | BUILD SUCCESSFUL，集成测试&崩溃修复完成 |
| 买家端布局简化重写 | ✅ 100% | 白色顶栏+共享底部导航+按钮不重叠 |
| 集成测试 | ✅ 100% | 详见 [接受引导书.md](接受引导书.md) |

## Android 客户端功能概览

| 功能模块 | Activity/Fragment | API 接口数 |
|----------|:-----------------:|:----------:|
| 登录/注册 | LoginActivity, RegisterActivity | 3 |
| 主框架 + 底部导航 | MainActivity, HomeFragment/BillsFragment/MessagesFragment/ProfileFragment | — |
| 快速记账 | SaleActivity | 1 |
| 草稿订单 | NewOrderActivity + Edit + EditQty | 5 |
| 商品管理 | ProductsActivity | 3 |
| 添加商品 | ProductAddActivity | 1 |
| 分类管理 | CategoriesActivity | 5 |
| 属性管理 | AttributesActivity | 5 |
| 进货入库 | PurchaseActivity | 3 |
| 经营分析 | AnalyticsActivity | 7 |
| 聊天 | ChatDetailActivity | 2 |
| 预约 | AppointmentsActivity | 3 |
| 智能体 | AgentPanelActivity | 1 |
| 买家端 | BuyerHome/Products/Follows/Messages/Profile | 2 |
| 数据管理 | DataManageActivity | （留接口）|

> 详细接手说明见 [接受引导书.md](接受引导书.md)

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
```

### 3. 启动后端

```bash
cd clothing_SI_server
mvn spring-boot:run
```

### 4. 启动 Android 客户端

1. 用 Android Studio 打开 `Clothing_SI_client/` 目录
2. 确认 `ApiClient.java` 中 `BASE_URL` 指向后端地址
3. 连接模拟器（`10.0.2.2`）或真机（局域网 IP）
4. 点击 Run

### 5. 访问

- API: `http://localhost:8080/api/...`
- 管理后台: `http://localhost:8080/admin/login`
- 管理员账号: `heben` / `heben0831`

### 6. 智能体配置

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
| Android 网络层 | Retrofit2 + Gson + TokenInterceptor |

## 技术栈

| 组件 | 版本 |
|:----|:----:|
| JDK | 17 |
| Spring Boot | 3.2.0 |
| MyBatis-Plus | 3.5.7 |
| MySQL | 8.0 |
| Android SDK | API 34 |
| Retrofit2 | 2.9.0 |
| Thymeleaf | 3.1.2 |
| Bootstrap | 5.3.3 |

---

*Clothing-SI V2.2 — HeBen_Cat*
