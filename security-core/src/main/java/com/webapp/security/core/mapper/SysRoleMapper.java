package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据角色编码查询角色
     */
    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode} AND deleted = 0")
    SysRole selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 查询角色的权限编码列
     */
    @Select("SELECT DISTINCT sp.perm_code " +
            "FROM sys_role sr " +
            "JOIN sys_role_permission srp ON sr.role_id = srp.role_id " +
            "JOIN sys_permission sp ON srp.permission_id = sp.permission_id " +
            "WHERE sr.role_id = #{roleId} " +
            "AND sr.status = 1 AND sr.deleted = 0 " +
            "AND sp.status = 1 AND sp.deleted = 0")
    List<String> selectRolePermissions(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询用户的角色列
     */
    @Select("SELECT sr.* " +
            "FROM sys_role sr " +
            "JOIN sys_user_role sur ON sr.role_id = sur.role_id " +
            "WHERE sur.user_id = #{userId} " +
            "AND sr.status = 1 AND sr.deleted = 0")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}

