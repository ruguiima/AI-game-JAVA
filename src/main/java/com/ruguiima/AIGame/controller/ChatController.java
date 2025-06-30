package com.ruguiima.AIGame.controller;

import com.ruguiima.AIGame.model.entity.ChatSession;
import com.ruguiima.AIGame.model.entity.User;
import com.ruguiima.AIGame.model.vo.MessageVO;
import com.ruguiima.AIGame.service.ChatService;
import com.ruguiima.AIGame.service.ChatSessionService;
import com.ruguiima.AIGame.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ChatController {
    @Autowired
    private ChatService chatService;
    
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
    
    @PostMapping("/chat")
    public String chat(@RequestParam String userMessage, 
                      @RequestParam(required = false) String sessionId,
                      Model model,
                      HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return "redirect:/login";
        }
        
        // 获取当前登录用户
        User currentUser = sessionService.getCurrentUser(httpSession);
        ChatSession session;
        
        // 使用临时会话或根据ID获取会话
        if (sessionId == null || sessionId.isEmpty()) {
            // 添加用户消息到临时会话或创建新临时会话
            session = chatSessionService.addMessage(null, "user", userMessage, currentUser);
        } else {
            // 添加用户消息到指定ID的会话
            session = chatSessionService.getSession(sessionId);
            if (session != null) {
                // 添加消息到已存在的会话
                if (session.getUser() != null && session.getUser().getId().equals(currentUser.getId())) {
                    // 用户自己的会话
                    session = chatSessionService.addMessage(sessionId, "user", userMessage, currentUser);
                } else if (session.getUser() == null) {
                    // 临时会话（理论上不会出现这种情况，因为所有会话都应该有用户关联）
                    session = chatSessionService.addMessage(sessionId, "user", userMessage);
                } else {
                    // 不是当前用户的会话，创建新临时会话
                    session = chatSessionService.addMessage(null, "user", userMessage, currentUser);
                }
            } else {
                // 会话ID无效，使用当前用户创建新临时会话
                session = chatSessionService.addMessage(null, "user", userMessage, currentUser);
            }
        }

        // 获取AI回复，传入会话以便处理多轮对话
        String reply = chatService.getMessage(session, userMessage);
        
        // 将AI回复添加到会话
        if (session.getUser() != null) {
            chatSessionService.addMessage(session.getSessionId(), "ai", reply, currentUser);
        } else {
            chatSessionService.addMessage(session.getSessionId(), "ai", reply);
        }
        
        // 重定向到会话页面，避免刷新页面时重复提交表单
        return "redirect:/session/" + session.getSessionId();
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
