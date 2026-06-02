package com.ai.service;

import com.ai.dto.OperationLog;
import com.ai.pojo.PageResult;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogService {
    void save(OperationLog logEntity);

    PageResult<OperationLog> getLogs(Long userId, String userName, String operation, LocalDateTime startTime, LocalDateTime endTime, int page, int size);

    void deleteByIds(List<Long> ids);
}