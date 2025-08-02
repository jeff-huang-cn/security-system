package com.webapp.security.admin.controller;

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
    public ResponseEntity<List<SysRole>> getAllRoles() {
        List<SysRole> roles = roleService.list();
        return ResponseEntity.ok(roles);
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseEntity<SysRole> getRoleById(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        return role != null ? ResponseEntity.ok(role) : ResponseEntity.notFound().build();
    }

    /**
     * 创建角色
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<String> createRole(@RequestBody SysRole role) {
        boolean success = roleService.createRole(role);
        return success ? ResponseEntity.ok("角色创建成功") : ResponseEntity.badRequest().body("角色创建失败");
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<String> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setRoleId(id);
        boolean success = roleService.updateRole(role);
        return success ? ResponseEntity.ok("角色更新成功") : ResponseEntity.badRequest().body("角色更新失败");
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseEntity<String> deleteRole(@PathVariable Long id) {
        boolean success = roleService.deleteRole(id);
        return success ? ResponseEntity.ok("角色删除成功") : ResponseEntity.badRequest().body("角色删除失败");
    }

    /**
     * 更新角色状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<String> updateRoleStatus(@PathVariable Long id, @RequestParam Integer status) {
        boolean success = roleService.updateRoleStatus(id, status);
        return success ? ResponseEntity.ok("角色状态更新成功") : ResponseEntity.badRequest().body("角色状态更新失败");
    }

    /**
     * 分配角色权限
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<String> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        boolean success = roleService.assignPermissions(id, permissionIds);
        return success ? ResponseEntity.ok("权限分配成功") : ResponseEntity.badRequest().body("权限分配失败");
    }

    /**
     * 获取角色权限
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseEntity<List<String>> getRolePermissions(@PathVariable Long id) {
        List<String> permissions = roleService.getRolePermissions(id);
        return ResponseEntity.ok(permissions);
    }

    /**
     * 获取启用的角色
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseEntity<List<SysRole>> getEnabledRoles() {
        List<SysRole> roles = roleService.getEnabledRoles();
        return ResponseEntity.ok(roles);
    }
}

