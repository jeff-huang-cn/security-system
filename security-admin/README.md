# 权限管理系统 - Admin模块

## 项目架构

本项目采用了以下架构设计：

### 目录结构

```
security-admin/
├── src/main/java/com/webapp/security/admin/
│   ├── controller/              # 控制器目录（按业务模块分类）
│   │   ├── user/                # 用户相关控制器
│   │   │   ├── UserController.java
│   │   │   ├── dto/             # 用户请求DTO
│   │   │   └── vo/              # 用户响应VO
│   │   ├── role/                # 角色相关控制器
│   │   ├── permission/          # 权限相关控制器
│   │   └── dashboard/           # 仪表盘相关控制器
│   ├── converter/               # MapStruct转换器
│   │   ├── UserConverter.java
│   │   ├── RoleConverter.java
│   │   └── PermissionConverter.java
│   └── facade/                  # 跨域业务融合服务
│       └── DashboardFacade.java
```

### 架构说明

1. **控制器层（Controller）**:
   - 按业务模块分包，每个模块有自己的DTO和VO
   - 负责请求接收、参数验证、权限检查
   - 通过Converter将DTO/VO与实体互转
   - 简单CRUD操作直接调用Service

2. **转换器层（Converter）**:
   - 使用MapStruct实现实体与DTO/VO之间的自动转换
   - 过滤敏感字段，增加展示字段

3. **门面层（Facade）**:
   - 用于组合多个Service的复杂业务流程
   - 处理跨领域数据聚合

4. **DTO/VO**:
   - DTO(Data Transfer Object): 接收前端请求数据
   - VO(View Object): 返回给前端的响应数据

## 重构内容

本次重构主要包括以下内容：

1. 重构目录结构，按业务模块进行分类
2. 引入DTO/VO模式，避免直接使用实体类与前端交互
3. 使用MapStruct实现高效的对象转换
4. 增加Facade层处理复杂业务逻辑
5. 规范化请求/响应处理

## 技术栈

- Spring Boot
- Spring Security
- Spring MVC
- MapStruct
- Lombok
- MySQL
- Flyway 