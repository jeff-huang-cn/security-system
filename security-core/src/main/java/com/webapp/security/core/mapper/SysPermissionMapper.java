package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统权限Mapper
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据权限编码查询权限
     */
    @Select("SELECT * FROM sys_permission WHERE perm_code = #{permCode} AND deleted = 0")
    SysPermission selectByPermCode(@Param("permCode") String permCode);

    /**
     * 根据父权限ID查询子权限列�?     */
    @Select("SELECT * FROM sys_permission WHERE parent_id = #{parentId} AND status = 1 AND deleted = 0 ORDER BY sort_order")
    List<SysPermission> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有启用的权限
     */
    @Select("SELECT * FROM sys_permission WHERE status = 1 AND deleted = 0 ORDER BY sort_order")
    List<SysPermission> selectAllEnabled();

    /**
     * 根据权限类型查询权限列表
     */
    @Select("SELECT * FROM sys_permission WHERE perm_type = #{permType} AND status = 1 AND deleted = 0 ORDER BY sort_order")
    List<SysPermission> selectByPermType(@Param("permType") Integer permType);

    /**
     * 根据角色ID查询权限列表
     */
    @Select("SELECT sp.* FROM sys_permission sp " +
            "JOIN sys_role_permission srp ON sp.permission_id = srp.permission_id " +
            "WHERE srp.role_id = #{roleId} " +
            "AND sp.status = 1 AND sp.deleted = 0 " +
            "ORDER BY sp.sort_order")
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询权限列表
     */
    @Select("SELECT DISTINCT sp.* FROM sys_permission sp " +
            "JOIN sys_role_permission srp ON sp.permission_id = srp.permission_id " +
            "JOIN sys_user_role sur ON srp.role_id = sur.role_id " +
            "WHERE sur.user_id = #{userId} " +
            "AND sp.status = 1 AND sp.deleted = 0 " +
            "ORDER BY sp.sort_order")
    List<SysPermission> selectByUserId(@Param("userId") Long userId);
}

