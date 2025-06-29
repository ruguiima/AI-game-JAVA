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
        ChatSession currentSession = chatSessionService.getCurrentSession();
        List<MessageVO> messages = chatSessionService.convertToMessageVOList(currentSession);
        
        // 获取会话ID，如果是临时会话则为null
        String sessionId = currentSession.getMessages().isEmpty() ? null : currentSession.getSessionId();
        
        model.addAttribute("messages", messages);
        model.addAttribute("sessions", chatSessionService.getAllSessions());
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
        // 获取或创建会话
        ChatSession session;
        
        // 使用临时会话或根据ID获取会话
        if (sessionId == null || sessionId.isEmpty()) {
            // 添加用户消息到当前会话或临时会话
            session = chatSessionService.addMessage(null, "user", userMessage);
        } else {
            // 添加用户消息到指定ID的会话
            session = chatSessionService.getSession(sessionId);
            if (session != null) {
                // 添加消息到已存在的会话
                chatSessionService.addMessage(sessionId, "user", userMessage);
            } else {
                // 会话ID无效，使用临时会话
                session = chatSessionService.addMessage(null, "user", userMessage);
            }
        }

        // 获取AI回复
        String reply = chatService.getMessage(userMessage);
        chatSessionService.addMessage(session.getSessionId(), "ai", reply);
        
        // 重定向到会话页面，避免刷新页面时重复提交表单
        return "redirect:/session/" + session.getSessionId();
    }
    
    @GetMapping("/session/{sessionId}")
    public String switchSession(@PathVariable String sessionId, Model model, HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return "redirect:/login";
        }
        ChatSession session = chatSessionService.setCurrentSession(sessionId);
        if (session == null) {
            session = chatSessionService.getCurrentSession();
        }
        
        // 将消息列表和会话信息传递给前端
        model.addAttribute("messages", chatSessionService.convertToMessageVOList(session));
        model.addAttribute("sessions", chatSessionService.getAllSessions());
        model.addAttribute("currentSessionId", session.getSessionId());
        
        return "index";
    }
    
    @GetMapping("/session/new")
    public String newSession(Model model, HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return "redirect:/login";
        }
        // 创建新的临时会话（不会添加到历史记录中，直到用户发送第一条消息）
        chatSessionService.createSession("新对话");
        
        model.addAttribute("messages", new ArrayList<>()); // 空消息列表
        model.addAttribute("sessions", chatSessionService.getAllSessions());
        model.addAttribute("currentSessionId", null); // 临时会话没有ID
        
        return "index";
    }
}
