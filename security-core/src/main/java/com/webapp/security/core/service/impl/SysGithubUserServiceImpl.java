package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysGithubUser;
import com.webapp.security.core.mapper.SysGithubUserMapper;
import com.webapp.security.core.service.SysGithubUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GitHub用户服务实现类
 */
@Service
public class SysGithubUserServiceImpl extends ServiceImpl<SysGithubUserMapper, SysGithubUser>
        implements SysGithubUserService {

    @Override
    public SysGithubUser getByGithubId(Long githubId) {
        return getOne(new LambdaQueryWrapper<SysGithubUser>()
                .eq(SysGithubUser::getGithubId, githubId));
    }

    @Override
    public SysGithubUser getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<SysGithubUser>()
                .eq(SysGithubUser::getUserId, userId));
    }

    @Override
    @Transactional
    public boolean bindUser(Long githubId, Long userId) {
        SysGithubUser githubUser = getByGithubId(githubId);
        if (githubUser == null) {
            return false;
        }

        // 设置用户ID并更新
        githubUser.setUserId(userId);
        return updateById(githubUser);
    }
}