package com.webapp.security.admin.controller;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<SysUser>> getAllUsers() {
        List<SysUser> users = userService.list();
        return ResponseEntity.ok(users);
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseEntity<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<String> createUser(@RequestBody SysUser user) {
        boolean success = userService.createUser(user);
        return success ? ResponseEntity.ok("用户创建成功") : ResponseEntity.badRequest().body("用户创建失败");
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        user.setUserId(id);
        boolean success = userService.updateUser(user);
        return success ? ResponseEntity.ok("用户更新成功") : ResponseEntity.badRequest().body("用户更新失败");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        return success ? ResponseEntity.ok("用户删除成功") : ResponseEntity.badRequest().body("用户删除失败");
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<String> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        boolean success = userService.updateUserStatus(id, status);
        return success ? ResponseEntity.ok("用户状态更新成功") : ResponseEntity.badRequest().body("用户状态更新失败");
    }

    /**
     * 分配用户角色
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<String> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        boolean success = userService.assignRoles(id, roleIds);
        return success ? ResponseEntity.ok("角色分配成功") : ResponseEntity.badRequest().body("角色分配失败");
    }

    /**
     * 获取用户权限
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseEntity<List<String>> getUserPermissions(@PathVariable Long id) {
        List<String> permissions = userService.getUserPermissions(id);
        return ResponseEntity.ok(permissions);
    }

    /**
     * 获取用户角色
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseEntity<List<String>> getUserRoles(@PathVariable Long id) {
        List<String> roles = userService.getUserRoles(id);
        return ResponseEntity.ok(roles);
    }
}

