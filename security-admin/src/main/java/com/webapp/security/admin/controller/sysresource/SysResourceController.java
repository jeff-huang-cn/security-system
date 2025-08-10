package com.webapp.security.admin.controller.sysresource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.webapp.security.admin.controller.sysresource.dto.ResourceDTO;
import com.webapp.security.admin.controller.sysresource.vo.ResourceVO;
import com.webapp.security.admin.converter.ResourceConverter;
import com.webapp.security.core.entity.SysResource;
import com.webapp.security.core.model.PagedDTO;
import com.webapp.security.core.model.PagedResult;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.hutool.core.util.StrUtil;

@RestController
@RequestMapping("/api/sys-resources")
@RequiredArgsConstructor
public class SysResourceController {

    private final SysResourceService resourceService;
    private final ResourceConverter resourceConverter;

    @PostMapping("/paged")
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_QUERY')")
    public ResponseResult<PagedResult<ResourceVO>> paged(@RequestBody PagedDTO paged) {
        Page<SysResource> page = new Page<>(paged.getPageNum(), paged.getPageSize());
        String keyword = paged.getKeyword();
        LambdaQueryWrapper<SysResource> qw = new LambdaQueryWrapper<SysResource>()
                .like(StrUtil.isNotBlank(keyword), SysResource::getResourceCode, keyword)
                .or(StrUtil.isNotBlank(keyword), c -> c.like(SysResource::getResourceName, keyword)
                        .or().like(SysResource::getResourcePath, keyword));
        Page<SysResource> result = resourceService.page(page, qw);
        return ResponseResult
                .success(new PagedResult<>(resourceConverter.toVOList(result.getRecords()), result.getTotal()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_QUERY')")
    public ResponseResult<List<ResourceVO>> all() {
        return ResponseResult.success(resourceConverter.toVOList(resourceService.list()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_CREATE')")
    public ResponseResult<Void> create(@Validated @RequestBody ResourceDTO dto) {
        SysResource res = resourceConverter.fromDTO(dto);
        return resourceService.save(res) ? ResponseResult.success(null, "创建成功") : ResponseResult.failed("创建失败");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_UPDATE')")
    public ResponseResult<Void> update(@PathVariable("id") Long id, @Validated @RequestBody ResourceDTO dto) {
        SysResource res = resourceService.getById(id);
        if (res == null)
            return ResponseResult.failed("不存在");
        resourceConverter.updateEntityFromDTO(dto, res);
        return resourceService.updateById(res) ? ResponseResult.success(null, "更新成功") : ResponseResult.failed("更新失败");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_DELETE')")
    public ResponseResult<Void> delete(@PathVariable("id") Long id) {
        return resourceService.removeById(id) ? ResponseResult.success(null, "删除成功") : ResponseResult.failed("删除失败");
    }
}