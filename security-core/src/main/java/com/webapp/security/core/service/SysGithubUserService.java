package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysGithubUser;

/**
 * GitHub用户服务接口
 */
public interface SysGithubUserService extends IService<SysGithubUser> {

    /**
     * 根据GitHub ID查询用户
     * 
     * @param githubId GitHub用户ID
     * @return GitHub用户信息
     */
    SysGithubUser getByGithubId(Long githubId);

    /**
     * 根据系统用户ID查询GitHub用户
     * 
     * @param userId 系统用户ID
     * @return GitHub用户信息
     */
    SysGithubUser getByUserId(Long userId);

    /**
     * 绑定GitHub用户到系统用户
     * 
     * @param githubId GitHub用户ID
     * @param userId   系统用户ID
     * @return 是否绑定成功
     */
    boolean bindUser(Long githubId, Long userId);
}