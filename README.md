# 笔记知识库系统 API 文档

## 项目介绍
本系统是一套支持多类型笔记（富文本文档、表格、画板/看板、思维导图）管理的知识库系统，提供用户认证、知识库管理、笔记创作与互动、搜索、后台数据统计等核心能力，所有接口遵循统一的响应格式规范。

## 接口基础信息
### 基础路径
所有接口的基础路径为：`/api`

### 认证方式
采用 JWT 认证，请求头需携带：`Authorization: Bearer <token>`  
（注：公开接口如通过分享Key访问笔记无需认证）

### 统一响应格式
所有接口返回 JSON 格式，结构如下：
```json
{
  "code": 1, // 1=成功，0=失败
  "msg": "success", // 提示信息
  "data": {} // 业务数据，失败时可能为null
}
```

### 笔记类型枚举
| 枚举值 | 说明         |
|--------|--------------|
| doc    | 富文本文档   |
| table  | 表格         |
| board  | 画板/看板    |
| mind   | 思维导图     |

## 核心功能模块
### 1. 用户认证与个人中心
管理用户注册、登录、个人信息维护等核心能力：
- **用户注册**：`POST /auth/register`
- **用户登录**：`POST /auth/login`（返回JWT token）
- **获取当前用户信息**：`GET /user/profile`（需认证）
- **修改个人资料**：`PUT /user/profile`（需认证）
- **修改密码**：`PUT /user/password`（需认证）
- **操作日志查询**：`GET /user/logs`（分页，需认证）
- **个人统计信息**：`GET /user/statistics`（创作/互动/活跃度等，需认证）

### 2. 知识库管理
支持知识库的创建、编辑、成员管理、导入导出等全生命周期管理：
- **创建知识库**：`POST /kb`（需认证）
- **我的知识库列表**：`GET /kb/my`（分页，需认证）
- **知识库详情（含笔记树）**：`GET /kb/{kbId}`（需认证）
- **更新知识库信息**：`PUT /kb/{kbId}`（需认证）
- **成员与邀请管理**：邀请发送/接受/拒绝、移除成员、修改角色（需认证）
- **导入导出**：
  - 知识库全量导出：`GET /kb/{kbId}/export`（`.lakebook` 文件流，需认证）
  - 文档导入：`POST /kb/{kbId}/import`（支持多格式批量上传，需编辑权限）
- **删除与恢复**：软删除、彻底删除、恢复知识库（需认证）

### 3. 笔记管理
支持4类笔记的创建、编辑、版本管理、分享、互动等操作：
- **创建笔记**：
  - 富文本文档：`POST /note/doc`
  - 表格：`POST /note/table`
  - 画板/看板：`POST /note/board`
  - 思维导图：`POST /note/mind`
- **笔记详情与编辑**：`GET /note/{noteId}`、`PUT /note/{noteId}`（自动生成版本，需认证）
- **版本管理**：版本列表、版本回滚（需认证）
- **回收站**：删除、恢复、彻底删除笔记（需认证）
- **分享与访问**：
  - 分享笔记：`POST /note/{noteId}/share`（支持密码/有效期/权限配置）
  - 匿名访问分享笔记：`GET /public/note/{shareKey}`（无需认证）
- **导出笔记**：`GET /note/{noteId}/export/{format}`（支持word/markdown/pdf等多格式，需认证）
- **互动操作**：点赞/取消点赞、收藏/取消收藏、记录浏览（需认证）

### 4. 笔记嵌套组件
支持在富文本文档中嵌入表格/画板/思维导图：
- 插入嵌套组件：`POST /doc/{docId}/embed`（需认证）
- 移除嵌套组件：`DELETE /doc/{docId}/embed/{embedId}`（需认证）
- 查询嵌套组件列表：`GET /doc/{docId}/embeds`（需认证）

### 5. 评论与互动
支持笔记的评论、回复、点赞操作：
- 发表评论/回复：`POST /comment`（需认证）
- 评论列表：`GET /note/{noteId}/comments`（分页，需认证）
- 删除评论：`DELETE /comment/{commentId}`（仅删除自己的评论，需认证）
- 评论点赞：`POST/DELETE /comment/{commentId}/like`（需认证）

### 6. 全局搜索
支持多维度筛选的全局搜索：
- 接口：`GET /search`（需认证）
- 筛选条件：关键词、类型、知识库、创建者、时间范围等（分页）

### 7. 首页/工作台
提供个性化工作台与推荐能力：
- 工作台数据：`GET /dashboard`（最近笔记、我的知识库、推荐知识库，需认证）
- 热门推荐：`GET /recommend/{type}/{limit}`（热门笔记/知识库，需认证）

### 8. 后台管理接口
仅限 ADMIN 角色访问，前缀 `/admin`：
- 数据概览：`GET /admin/statistics/overview`（总用户/笔记/知识库等）
- 年度趋势：`GET /admin/statistics/yearly`（按月统计）
- 月度汇总：`GET /admin/statistics/monthly`（单月汇总）
- 刷新统计：`POST /admin/statistics/refresh`（刷新指定日期的每日统计）

## 接口调用示例
### 示例1：用户登录
```bash
curl -X POST \
  http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "zhangsan",
    "password": "123456"
  }'
```
响应示例：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userInfo": {
      "id": 1,
      "name": "zhangsan",
      "role": "USER"
    }
  }
}
```

### 示例2：创建富文本文档（需认证）
```bash
curl -X POST \
  http://localhost:8080/api/note/doc \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIs...' \
  -H 'Content-Type: application/json' \
  -d '{
    "kbId": 1,
    "parentDocId": null,
    "title": "SpringBoot入门",
    "content": "# SpringBoot\n## 自动配置原理",
    "type": "doc"
  }'
```

## 权限说明
- 普通用户（USER）：仅可操作自己创建的知识库/笔记，或被邀请的知识库（按角色权限）
- 管理员（ADMIN）：可访问 `/admin` 前缀接口，查看全量数据统计
- 笔记/知识库权限：查看、编辑、所有者（OWNER/ADMIN/EDITOR/VIEWER）

## 补充说明
1. 文件导入支持格式：`.lakebook`（知识库）、`.lake`（单笔记）、`.docx/.md/.xlsx` 等通用格式
2. 笔记导出支持格式：word、markdown、pdf、lake、jpg、xlsx（按笔记类型适配）
3. 所有分页接口默认页码 `page=1`，每页条数 `size=20`
4. 时间格式统一采用 ISO 8601 或 `YYYY-MM-DD HH:mm:ss`
