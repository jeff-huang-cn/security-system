package com.webapp.security.admin.facade;

import com.webapp.security.admin.controller.dashboard.vo.DashboardStatVO;
import com.webapp.security.admin.converter.UserConverter;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.service.SysPermissionService;
import com.webapp.security.core.service.SysRoleService;
import com.webapp.security.core.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 仪表盘数据聚合服务
 */
@Service
@RequiredArgsConstructor
public class DashboardFacade {

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysPermissionService permissionService;
    private final UserConverter userConverter;

    /**
     * 获取仪表盘统计数据
     */
    public DashboardStatVO getDashboardStats() {
        DashboardStatVO stats = new DashboardStatVO();

        // 获取用户总数
        stats.setTotalUsers(userService.count());

        // 获取活跃用户数（状态为启用的用户）
        stats.setActiveUsers(countActiveUsers());

        // 获取角色总数
        stats.setTotalRoles(roleService.count());

        // 获取权限总数
        stats.setTotalPermissions(permissionService.count());

        // 获取最近7天新增的用户
        List<SysUser> recentUsers = getRecentUsers(7);
        stats.setRecentUsers(userConverter.toVOList(recentUsers));

        return stats;
    }

    /**
     * 统计活跃用户数量
     */
    private long countActiveUsers() {
        return userService.list().stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .count();
    }

    /**
     * 获取最近几天新增的用户
     */
    private List<SysUser> getRecentUsers(int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return userService.list().stream()
                .filter(user -> user.getCreateTime() != null && user.getCreateTime().isAfter(startDate))
                .limit(10) // 最多返回10个用户
                .collect(java.util.stream.Collectors.toList());
    }
}