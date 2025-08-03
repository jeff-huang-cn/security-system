package com.webapp.security.admin.controller;

import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.entity.SysRole;
import com.webapp.security.core.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    @Autowired
    private SysRoleService roleService;

    /**
     * 获取所有角色
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<List<SysRole>> getAllRoles() {
        List<SysRole> roles = roleService.list();
        return ResponseResult.success(roles);
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<SysRole> getRoleById(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        return role != null ? ResponseResult.success(role) : ResponseResult.failed("角色不存在");
    }

    /**
     * 创建角色
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseResult<Void> createRole(@RequestBody SysRole role) {
        boolean success = roleService.createRole(role);
        return success ? ResponseResult.success(null, "角色创建成功") : ResponseResult.failed("角色创建失败");
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseResult<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setRoleId(id);
        boolean success = roleService.updateRole(role);
        return success ? ResponseResult.success(null, "角色更新成功") : ResponseResult.failed("角色更新失败");
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseResult<Void> deleteRole(@PathVariable Long id) {
        boolean success = roleService.deleteRole(id);
        return success ? ResponseResult.success(null, "角色删除成功") : ResponseResult.failed("角色删除失败");
    }

    /**
     * 更新角色状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseResult<Void> updateRoleStatus(@PathVariable Long id, @RequestBody StatusRequest statusRequest) {
        boolean success = roleService.updateRoleStatus(id, statusRequest.getStatus());
        return success ? ResponseResult.success(null, "角色状态更新成功") : ResponseResult.failed("角色状态更新失败");
    }

    /**
     * 分配角色权限
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseResult<Void> assignPermissions(@PathVariable Long id, @RequestBody PermissionIdsRequest request) {
        boolean success = roleService.assignPermissions(id, request.getPermissionIds());
        return success ? ResponseResult.success(null, "权限分配成功") : ResponseResult.failed("权限分配失败");
    }

    /**
     * 获取角色权限
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<List<String>> getRolePermissions(@PathVariable Long id) {
        List<String> permissions = roleService.getRolePermissions(id);
        return ResponseResult.success(permissions);
    }

    /**
     * 获取启用的角色
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<List<SysRole>> getEnabledRoles() {
        List<SysRole> roles = roleService.getEnabledRoles();
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
     * 权限ID请求类
     */
    public static class PermissionIdsRequest {
        private List<Long> permissionIds;

        public List<Long> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }
}
