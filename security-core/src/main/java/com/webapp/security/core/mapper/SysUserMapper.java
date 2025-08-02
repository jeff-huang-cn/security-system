package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用�?     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = 0")
    SysUser selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted = 0")
    SysUser selectByPhone(@Param("phone") String phone);

    /**
     * 查询用户的权限编码列
     */
    @Select("SELECT DISTINCT sp.perm_code " +
            "FROM sys_user su " +
            "JOIN sys_user_role sur ON su.user_id = sur.user_id " +
            "JOIN sys_role sr ON sur.role_id = sr.role_id " +
            "JOIN sys_role_permission srp ON sr.role_id = srp.role_id " +
            "JOIN sys_permission sp ON srp.permission_id = sp.permission_id " +
            "WHERE su.user_id = #{userId} " +
            "AND su.status = 1 AND su.deleted = 0 " +
            "AND sr.status = 1 AND sr.deleted = 0 " +
            "AND sp.status = 1 AND sp.deleted = 0")
    List<String> selectUserPermissions(@Param("userId") Long userId);

    /**
     * 查询用户的角色编码列
     */
    @Select("SELECT DISTINCT sr.role_code " +
            "FROM sys_user su " +
            "JOIN sys_user_role sur ON su.user_id = sur.user_id " +
            "JOIN sys_role sr ON sur.role_id = sr.role_id " +
            "WHERE su.user_id = #{userId} " +
            "AND su.status = 1 AND su.deleted = 0 " +
            "AND sr.status = 1 AND sr.deleted = 0")
    List<String> selectUserRoles(@Param("userId") Long userId);
}

