package com.ai.mapper;

import com.ai.dto.KbFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// 4. KbFileMapper
public interface KbFileMapper {
    void insert(KbFile file);
}