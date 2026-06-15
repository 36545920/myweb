package com.myweb.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myweb.model.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper extends BaseMapper<FileEntity> {
}
