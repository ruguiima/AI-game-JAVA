package com.ruguiima.nexus.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ruguiima.nexus.model.entity.ChatSession;
import com.ruguiima.nexus.model.entity.User;
import com.ruguiima.nexus.model.vo.MessageVO;
import com.ruguiima.nexus.service.ChatSessionService;
import com.ruguiima.nexus.service.SessionService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ChatController {
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private SessionService sessionService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        
        User currentUser = sessionService.getCurrentUser(session);
        ChatSession currentSession = chatSessionService.getCurrentSession(currentUser);
        List<MessageVO> messages = chatSessionService.convertToMessageVOList(currentSession);
        
        // 获取会话ID，如果是临时会话则为null
        String sessionId = currentSession.getMessages().isEmpty() ? null : currentSession.getSessionId();
        
        model.addAttribute("messages", messages);
        model.addAttribute("sessions", chatSessionService.getUserSessions(currentUser));
        model.addAttribute("currentSessionId", sessionId);
        model.addAttribute("user", currentUser); // 添加当前用户信息
        return "index";
    }
    
    @GetMapping("/session/{sessionId}")
    public String switchSession(@PathVariable String sessionId, Model model, HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return "redirect:/login";
        }
        
        // 处理特殊情况：如果是新会话路径
        if ("new".equals(sessionId)) {
            return newSession(model, httpSession);
        }
        
        User currentUser = sessionService.getCurrentUser(httpSession);
        ChatSession session = chatSessionService.setCurrentSession(sessionId, currentUser);
        
        if (session == null) {
            session = chatSessionService.getCurrentSession(currentUser);
        }
        
        // 将消息列表和会话信息传递给前端
        model.addAttribute("messages", chatSessionService.convertToMessageVOList(session));
        model.addAttribute("sessions", chatSessionService.getUserSessions(currentUser));
        model.addAttribute("currentSessionId", session.getSessionId());
        model.addAttribute("user", currentUser);
        
        return "index";
    }
    
    @GetMapping("/session/new")
    public String newSession(Model model, HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return "redirect:/login";
        }
        
        User currentUser = sessionService.getCurrentUser(httpSession);
        
        // 创建临时会话（不保存到数据库，只有用户发送消息后才保存）
        ChatSession tempSession = new ChatSession("新对话");
        tempSession.setUser(currentUser); // 设置用户，但不保存到数据库
        chatSessionService.setTempSession(tempSession); // 设置为当前临时会话
        
        model.addAttribute("messages", new ArrayList<>()); // 空消息列表
        model.addAttribute("sessions", chatSessionService.getUserSessions(currentUser));
        model.addAttribute("currentSessionId", null); // 临时会话没有ID传递给前端
        model.addAttribute("user", currentUser);
        
        return "index";
    }
}
