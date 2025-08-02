-- 权限管理系统数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS webapp_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE webapp_auth;

-- 系统用户表
CREATE TABLE sys_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '状态（1-启用 0-禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志（0-未删除 1-已删除）',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 系统角色表
CREATE TABLE sys_role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态（1-启用 0-禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志（0-未删除 1-已删除）',
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 系统权限表
CREATE TABLE sys_permission (
    permission_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    type TINYINT NOT NULL COMMENT '权限类型（1-菜单 2-按钮 3-接口）',
    parent_id BIGINT COMMENT '父权限ID',
    path VARCHAR(200) COMMENT '权限路径',
    description VARCHAR(200) COMMENT '权限描述',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态（1-启用 0-禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志（0-未删除 1-已删除）',
    INDEX idx_code (code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) COMMENT '创建人',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) COMMENT '创建人',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id),
    FOREIGN KEY (role_id) REFERENCES sys_role(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES sys_permission(permission_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 插入初始数据

-- 插入超级管理员用户（密码：admin123）
INSERT INTO sys_user (username, password, real_name, email, status, create_by) VALUES 
('admin', '$2a$10$7JB720yubVSOfvVWbazBuOWShWvheWjxVYaGYoUaxMNKiGqipcl2.', '超级管理员', 'admin@webapp.com', 1, 'system');

-- 插入角色
INSERT INTO sys_role (code, name, description, create_by) VALUES 
('SUPER_ADMIN', '超级管理员', '系统超级管理员，拥有所有权限', 'system'),
('ADMIN', '管理员', '系统管理员', 'system'),
('USER', '普通用户', '普通用户', 'system');

-- 插入权限
INSERT INTO sys_permission (code, name, type, parent_id, path, description, sort, create_by) VALUES 
-- 系统管理
('system', '系统管理', 1, NULL, '/system', '系统管理模块', 1, 'system'),
('system:user', '用户管理', 1, 1, '/system/user', '用户管理页面', 1, 'system'),
('system:user:list', '用户列表', 2, 2, NULL, '查看用户列表', 1, 'system'),
('system:user:add', '新增用户', 2, 2, NULL, '新增用户', 2, 'system'),
('system:user:edit', '编辑用户', 2, 2, NULL, '编辑用户', 3, 'system'),
('system:user:delete', '删除用户', 2, 2, NULL, '删除用户', 4, 'system'),
('system:role', '角色管理', 1, 1, '/system/role', '角色管理页面', 2, 'system'),
('system:role:list', '角色列表', 2, 7, NULL, '查看角色列表', 1, 'system'),
('system:role:add', '新增角色', 2, 7, NULL, '新增角色', 2, 'system'),
('system:role:edit', '编辑角色', 2, 7, NULL, '编辑角色', 3, 'system'),
('system:role:delete', '删除角色', 2, 7, NULL, '删除角色', 4, 'system'),
('system:permission', '权限管理', 1, 1, '/system/permission', '权限管理页面', 3, 'system'),
('system:permission:list', '权限列表', 2, 12, NULL, '查看权限列表', 1, 'system'),
('system:permission:add', '新增权限', 2, 12, NULL, '新增权限', 2, 'system'),
('system:permission:edit', '编辑权限', 2, 12, NULL, '编辑权限', 3, 'system'),
('system:permission:delete', '删除权限', 2, 12, NULL, '删除权限', 4, 'system');

-- 分配超级管理员角色给admin用户
INSERT INTO sys_user_role (user_id, role_id, create_by) VALUES (1, 1, 'system');

-- 分配所有权限给超级管理员角色
INSERT INTO sys_role_permission (role_id, permission_id, create_by) 
SELECT 1, permission_id, 'system' FROM sys_permission WHERE deleted = 0;