package com.webapp.security.admin.controller;

import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<SysPermission>> getAllPermissions() {
        List<SysPermission> permissions = permissionService.list();
        return ResponseEntity.ok(permissions);
    }

    /**
     * 根据ID获取权限
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseEntity<SysPermission> getPermissionById(@PathVariable Long id) {
        SysPermission permission = permissionService.getById(id);
        return permission != null ? ResponseEntity.ok(permission) : ResponseEntity.notFound().build();
    }

    /**
     * 创建权限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    public ResponseEntity<String> createPermission(@RequestBody SysPermission permission) {
        boolean success = permissionService.createPermission(permission);
        return success ? ResponseEntity.ok("权限创建成功") : ResponseEntity.badRequest().body("权限创建失败");
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<String> updatePermission(@PathVariable Long id, @RequestBody SysPermission permission) {
        permission.setPermissionId(id);
        boolean success = permissionService.updatePermission(permission);
        return success ? ResponseEntity.ok("权限更新成功") : ResponseEntity.badRequest().body("权限更新失败");
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    public ResponseEntity<String> deletePermission(@PathVariable Long id) {
        boolean success = permissionService.deletePermission(id);
        return success ? ResponseEntity.ok("权限删除成功") : ResponseEntity.badRequest().body("权限删除失败");
    }

    /**
     * 更新权限状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<String> updatePermissionStatus(@PathVariable Long id, @RequestParam Integer status) {
        boolean success = permissionService.updatePermissionStatus(id, status);
        return success ? ResponseEntity.ok("权限状态更新成功") : ResponseEntity.badRequest().body("权限状态更新失败");
    }

    /**
     * 获取菜单权限树
     */
    @GetMapping("/menu-tree")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseEntity<List<SysPermission>> getMenuPermissions() {
        List<SysPermission> permissions = permissionService.getMenuPermissions();
        return ResponseEntity.ok(permissions);
    }

    /**
     * 获取子权限
     */
    @GetMapping("/{parentId}/children")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseEntity<List<SysPermission>> getChildPermissions(@PathVariable Long parentId) {
        List<SysPermission> permissions = permissionService.getChildPermissions(parentId);
        return ResponseEntity.ok(permissions);
    }

    /**
     * 获取启用的权限
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseEntity<List<SysPermission>> getEnabledPermissions() {
        List<SysPermission> permissions = permissionService.getEnabledPermissions();
        return ResponseEntity.ok(permissions);
    }
}

