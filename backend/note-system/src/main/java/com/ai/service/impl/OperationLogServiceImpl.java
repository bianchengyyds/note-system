package com.ai.service.impl;

import com.ai.dto.OperationLog;
import com.ai.mapper.OperationLogMapper;
import com.ai.pojo.PageResult;
import com.ai.service.OperationLogService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {
    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public void save(OperationLog logEntity) {
        operationLogMapper.insert(logEntity);
    }

    @Override
    public PageResult<OperationLog> getLogs(Long userId, String userName, String operation, LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        PageHelper.startPage(page, size);
        List<OperationLog> list = operationLogMapper.selectLogs(userId, userName, operation, startTime, endTime);
        PageInfo<OperationLog> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), page, size, list);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        operationLogMapper.deleteByIds(ids);
    }
}