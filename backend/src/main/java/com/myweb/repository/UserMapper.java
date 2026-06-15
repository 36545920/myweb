package com.myweb.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myweb.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
