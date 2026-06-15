package com.myweb.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myweb.model.entity.FileTransfer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileTransferMapper extends BaseMapper<FileTransfer> {
}
