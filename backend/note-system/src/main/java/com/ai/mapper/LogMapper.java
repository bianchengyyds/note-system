package com.ai.mapper;

import com.ai.pojo.LogInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogMapper {
    List<LogInfo> list(@Param("userId") Long userId);
}
