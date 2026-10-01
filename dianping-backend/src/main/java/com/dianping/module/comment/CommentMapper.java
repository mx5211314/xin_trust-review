package com.dianping.module.comment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.module.comment.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
