package com.webapp.security.admin.controller;

import com.webapp.security.core.model.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * 测试接口
     */
    @GetMapping("/hello")
    public ResponseResult<String> hello() {
        return ResponseResult.success("Hello from Permission Admin!");
    }
}