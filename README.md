# 基于 SpringBoot 的学习笔记整理系统

> 基于 SpringBoot + Vue3 开发的前后端分离在线笔记/知识库系统，支持富文本、表格、画板、思维导图四种笔记类型，具备权限管理、分享、导入导出、版本管理等企业级功能。

---

## 🌟 项目功能
- 用户注册、登录、JWT 认证
- 个人资料、操作日志、个人统计
- 多类型笔记：富文本 / 表格 / 画板 / 思维导图
- 知识库管理（创建、编辑、删除、成员权限）
- 笔记版本管理、回滚、回收站
- 笔记分享（密码、有效期、权限）
- 评论、点赞、收藏、浏览历史
- 全局搜索、工作台、热门推荐
- 管理员后台：用户管理、内容审核、数据统计

---

## 🛠 技术栈
### 后端
- Java 8+
- SpringBoot
- Spring Security + JWT
- MySQL
- MyBatis-Plus
- Maven

### 前端
- Vue3
- Vite
- Element Plus
- Axios

---

## 📁 项目结构
NoteSystem/
├── backend/ # 后端代码（SpringBoot）
├── frontend/ # 前端代码（Vue3）
├── README.md # 项目说明文档
└── 接口文档.md # 完整 API 接口文档


---

## 🚀 快速启动

### 1. 后端启动
1. 创建 MySQL 数据库：`notesystem`
2. 执行项目中的 SQL 初始化脚本
3. 修改 `application.yml` 中的数据库连接信息
4. 启动 `BackendApplication` 主类

### 2. 前端启动
```bash
cd frontend
npm install
npm run dev
```

📖 接口文档

详细接口说明（请求方式、参数、响应、权限）已单独整理：
👉 ./ 接口文档.md

📌 访问地址

前端地址：http://localhost:5173

后端接口：http://localhost:8080/api

接口文档：./ 接口文档.md

👨‍💻 作者

GitHub：bianchengyyds
