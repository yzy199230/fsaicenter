package com.fsa.aicenter.application.dto.request;

import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新状态请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "更新状态请求")
public class UpdateStatusRequest {

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态", example = "ENABLED")
    private EntityStatus status;
}
