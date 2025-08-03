package com.webapp.security.admin.controller;

import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private SysUserService userService;

    /**
     * 获取所有用户
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<List<SysUser>> getAllUsers() {
        List<SysUser> users = userService.list();
        return ResponseResult.success(users);
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        return user != null ? ResponseResult.success(user) : ResponseResult.failed("用户不存在");
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseResult<Void> createUser(@RequestBody SysUser user) {
        boolean success = userService.createUser(user);
        return success ? ResponseResult.success(null, "用户创建成功") : ResponseResult.failed("用户创建失败");
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseResult<Void> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        user.setUserId(id);
        boolean success = userService.updateUser(user);
        return success ? ResponseResult.success(null, "用户更新成功") : ResponseResult.failed("用户更新失败");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseResult<Void> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        return success ? ResponseResult.success(null, "用户删除成功") : ResponseResult.failed("用户删除失败");
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseResult<Void> updateUserStatus(@PathVariable Long id, @RequestBody StatusRequest statusRequest) {
        boolean success = userService.updateUserStatus(id, statusRequest.getStatus());
        return success ? ResponseResult.success(null, "用户状态更新成功") : ResponseResult.failed("用户状态更新失败");
    }

    /**
     * 分配用户角色
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseResult<Void> assignRoles(@PathVariable Long id, @RequestBody RoleIdsRequest request) {
        boolean success = userService.assignRoles(id, request.getRoleIds());
        return success ? ResponseResult.success(null, "角色分配成功") : ResponseResult.failed("角色分配失败");
    }

    /**
     * 获取用户权限
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<List<String>> getUserPermissions(@PathVariable Long id) {
        List<String> permissions = userService.getUserPermissions(id);
        return ResponseResult.success(permissions);
    }

    /**
     * 获取用户角色
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<List<String>> getUserRoles(@PathVariable Long id) {
        List<String> roles = userService.getUserRoles(id);
        return ResponseResult.success(roles);
    }

    /**
     * 状态请求类
     */
    public static class StatusRequest {
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    /**
     * 角色ID请求类
     */
    public static class RoleIdsRequest {
        private List<Long> roleIds;

        public List<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
        }
    }
}
