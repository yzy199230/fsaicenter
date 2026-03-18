package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.LogListQuery;
import com.fsa.aicenter.application.dto.response.LogDetailResponse;
import com.fsa.aicenter.application.dto.response.LogListResponse;
import com.fsa.aicenter.application.service.LogQueryService;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 日志查询Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "日志查询", description = "请求日志查询接口")
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogQueryService logQueryService;

    @Operation(summary = "查询日志列表")
    @RequirePermission("log:list")
    @GetMapping
    public Result<PageResult<LogListResponse>> listLogs(LogListQuery query) {
        PageResult<LogListResponse> pageResult = logQueryService.listLogs(query);
        return Result.success(pageResult);
    }

    @Operation(summary = "查询日志详情")
    @RequirePermission("log:view")
    @GetMapping("/{id}")
    public Result<LogDetailResponse> getLogDetail(
            @Parameter(description = "日志ID") @PathVariable Long id) {
        LogDetailResponse detail = logQueryService.getLogDetail(id);
        return Result.success(detail);
    }
}
