package com.ai.mapper;

import com.ai.dto.ShareDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShareMapper {
    // 新增分享
    void insert(ShareDTO shareDTO);

    // 根据shareKey查询分享
    int countByShareKey(String shareKey);

    // 根据shareKey查询分享
    ShareDTO selectByShareKey(String shareKey);
}
