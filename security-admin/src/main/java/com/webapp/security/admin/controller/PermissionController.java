package com.webapp.security.admin.controller;

import com.webapp.security.core.model.ErrorCode;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 */
@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*")
public class PermissionController {

    @Autowired
    private SysPermissionService permissionService;

    /**
     * 获取所有权限
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<List<SysPermission>> getAllPermissions() {
        List<SysPermission> permissions = permissionService.list();
        return ResponseResult.success(permissions);
    }

    /**
     * 根据ID获取权限
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<SysPermission> getPermissionById(@PathVariable Long id) {
        SysPermission permission = permissionService.getById(id);
        return permission != null ? ResponseResult.success(permission) : ResponseResult.failed(ErrorCode.PERMISSION_NOT_FOUND);
    }

    /**
     * 创建权限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    public ResponseResult<Void> createPermission(@RequestBody SysPermission permission) {
        boolean success = permissionService.createPermission(permission);
        return success ? ResponseResult.success(null, "权限创建成功") : ResponseResult.failed("权限创建失败");
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    public ResponseResult<Void> updatePermission(@PathVariable Long id, @RequestBody SysPermission permission) {
        permission.setPermissionId(id);
        boolean success = permissionService.updatePermission(permission);
        return success ? ResponseResult.success(null, "权限更新成功") : ResponseResult.failed("权限更新失败");
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    public ResponseResult<Void> deletePermission(@PathVariable Long id) {
        boolean success = permissionService.deletePermission(id);
        return success ? ResponseResult.success(null, "权限删除成功") : ResponseResult.failed("权限删除失败");
    }

    /**
     * 更新权限状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    public ResponseResult<Void> updatePermissionStatus(@PathVariable Long id,
            @RequestBody StatusRequest statusRequest) {
        boolean success = permissionService.updatePermissionStatus(id, statusRequest.getStatus());
        return success ? ResponseResult.success(null, "权限状态更新成功") : ResponseResult.failed("权限状态更新失败");
    }

    /**
     * 获取菜单权限树
     */
    @GetMapping("/menu-tree")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<List<SysPermission>> getMenuPermissions() {
        List<SysPermission> permissions = permissionService.getMenuPermissions();
        return ResponseResult.success(permissions);
    }

    /**
     * 获取子权限
     */
    @GetMapping("/{parentId}/children")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<List<SysPermission>> getChildPermissions(@PathVariable Long parentId) {
        List<SysPermission> permissions = permissionService.getChildPermissions(parentId);
        return ResponseResult.success(permissions);
    }

    /**
     * 获取启用的权限
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<List<SysPermission>> getEnabledPermissions() {
        List<SysPermission> permissions = permissionService.getEnabledPermissions();
        return ResponseResult.success(permissions);
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
}
