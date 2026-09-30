package com.dianping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    /** 点赞数同步任务使用：直接覆盖为 Redis 快照值 */
    @Update("UPDATE content SET like_count = #{count} WHERE id = #{id}")
    int updateLikeCount(@Param("id") Long id, @Param("count") long count);

    /** 浏览计数 +1（详情页每次打开上报） */
    @Update("UPDATE content SET view_count = view_count + 1 WHERE id = #{id}")
    int incrView(@Param("id") Long id);

    /** 用户数据三卡：发布数 / 获赞总数 / 浏览总量（仅统计上架内容） */
    @Select("SELECT COUNT(*) AS posts, IFNULL(SUM(like_count),0) AS likes, " +
            "IFNULL(SUM(view_count),0) AS views FROM content " +
            "WHERE user_id = #{uid} AND status = 'APPROVED' AND deleted = 0")
    java.util.Map<String, Object> statsOfUser(@Param("uid") Long uid);
}
