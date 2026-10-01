package com.dianping.module.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.module.chat.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /** 会话列表：与我有过私信的对方 id + 该会话最后一条消息 id，按最后消息倒序 */
    @Select("SELECT CASE WHEN from_user_id = #{me} THEN to_user_id ELSE from_user_id END AS peer_id, " +
            "MAX(id) AS last_id FROM message " +
            "WHERE from_user_id = #{me} OR to_user_id = #{me} " +
            "GROUP BY peer_id ORDER BY last_id DESC LIMIT #{limit}")
    List<Map<String, Object>> conversations(@Param("me") Long me, @Param("limit") int limit);

    /** 对方发给我且未读的条数 */
    @Select("SELECT COUNT(*) FROM message WHERE from_user_id = #{peer} AND to_user_id = #{me} AND is_read = 0")
    long unreadFrom(@Param("peer") Long peer, @Param("me") Long me);
}
