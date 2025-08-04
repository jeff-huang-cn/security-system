package com.webapp.security.admin.controller.dashboard;

import com.webapp.security.admin.controller.dashboard.vo.DashboardStatVO;
import com.webapp.security.admin.facade.DashboardFacade;
import com.webapp.security.core.model.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘控制器
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardFacade dashboardFacade;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseResult<DashboardStatVO> getDashboardStats() {
        DashboardStatVO stats = dashboardFacade.getDashboardStats();
        return ResponseResult.success(stats);
    }
}