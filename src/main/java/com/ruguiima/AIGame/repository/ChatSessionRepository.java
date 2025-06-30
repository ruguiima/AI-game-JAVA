package com.ruguiima.AIGame.repository;

import com.ruguiima.AIGame.model.entity.ChatSession;
import com.ruguiima.AIGame.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    /**
     * 根据用户查找聊天会话列表
     * @param user 用户对象
     * @return 用户的所有聊天会话
     */
    List<ChatSession> findByUserOrderByLastUpdatedTimeDesc(User user);
    
    /**
     * 根据用户ID查找聊天会话列表
     * @param userId 用户ID
     * @return 用户的所有聊天会话
     */
    List<ChatSession> findByUserId(Long userId);
    
    /**
     * 根据会话ID和用户查找会话
     * @param sessionId 会话ID
     * @param user 用户对象
     * @return 用户的指定会话
     */
    ChatSession findBySessionIdAndUser(String sessionId, User user);
}
