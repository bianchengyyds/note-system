package com.ai.mapper;

import com.ai.dto.OperationLog;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {
    // 插入操作日志
    void insert(OperationLog logEntity);

    // 查询操作日志
    List<OperationLog> selectLogs(Long userId, String userName, String operation, LocalDateTime startTime, LocalDateTime endTime);

    // 删除操作日志
    void deleteByIds(List<Long> ids);
}