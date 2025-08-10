package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysResource;
import com.webapp.security.core.mapper.SysResourceMapper;
import com.webapp.security.core.service.SysResourceService;
import org.springframework.stereotype.Service;

@Service
public class SysResourceServiceImpl extends ServiceImpl<SysResourceMapper, SysResource> implements SysResourceService {
}