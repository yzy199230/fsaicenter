package com.fsa.aicenter.infrastructure.persistence.repository;

import com.fsa.aicenter.domain.admin.aggregate.AdminOperationLog;
import com.fsa.aicenter.domain.admin.repository.AdminOperationLogRepository;
import com.fsa.aicenter.infrastructure.persistence.mapper.AdminOperationLogMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminOperationLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 操作日志仓储实现
 *
 * @author FSA AI Center
 */
@Repository
@RequiredArgsConstructor
public class AdminOperationLogRepositoryImpl implements AdminOperationLogRepository {

    private final AdminOperationLogMapper mapper;

    @Override
    public AdminOperationLog save(AdminOperationLog log) {
        AdminOperationLogPO po = toPO(log);
        mapper.insert(po);
        log.setId(po.getId());
        return log;
    }

    @Override
    public Optional<AdminOperationLog> findById(Long id) {
        AdminOperationLogPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AdminOperationLog> findByUserId(Long userId, int offset, int limit) {
        List<AdminOperationLogPO> pos = mapper.findByUserId(userId, offset, limit);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AdminOperationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, int offset, int limit) {
        List<AdminOperationLogPO> pos = mapper.findByTimeRange(startTime, endTime, offset, limit);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return mapper.selectCount(null);
    }

    private AdminOperationLog toDomain(AdminOperationLogPO po) {
        return AdminOperationLog.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .username(po.getUsername())
                .operation(po.getOperation())
                .method(po.getMethod())
                .params(po.getParams())
                .ip(po.getIp())
                .location(po.getLocation())
                .userAgent(po.getUserAgent())
                .executeTime(po.getExecuteTime())
                .status(po.getStatus())
                .errorMsg(po.getErrorMsg())
                .createdTime(po.getCreatedTime())
                .build();
    }

    private AdminOperationLogPO toPO(AdminOperationLog log) {
        AdminOperationLogPO po = new AdminOperationLogPO();
        po.setId(log.getId());
        po.setUserId(log.getUserId());
        po.setUsername(log.getUsername());
        po.setOperation(log.getOperation());
        po.setMethod(log.getMethod());
        po.setParams(log.getParams());
        po.setIp(log.getIp());
        po.setLocation(log.getLocation());
        po.setUserAgent(log.getUserAgent());
        po.setExecuteTime(log.getExecuteTime());
        po.setStatus(log.getStatus());
        po.setErrorMsg(log.getErrorMsg());
        return po;
    }
}
