package com.dianping.module.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.module.user.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
