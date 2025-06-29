package com.ruguiima.AIGame.service;

import com.ruguiima.AIGame.model.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    
    // 用户会话中的键
    private static final String USER_SESSION_KEY = "currentUser";
    
    /**
     * 将用户信息存入会话
     */
    public void saveUserToSession(HttpSession session, User user) {
        session.setAttribute(USER_SESSION_KEY, user);
    }
    
    /**
     * 从会话中获取当前登录用户
     */
    public User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute(USER_SESSION_KEY);
    }
    
    /**
     * 检查用户是否已登录
     */
    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(USER_SESSION_KEY) != null;
    }
    
    /**
     * 退出登录
     */
    public void logout(HttpSession session) {
        session.removeAttribute(USER_SESSION_KEY);
    }
}
