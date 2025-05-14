package com.ruguiima.AIGame.controller;

import com.ruguiima.AIGame.model.vo.MessageVO;
import com.ruguiima.AIGame.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ChatController {
    @Autowired
    private ChatService chatService;

    private final List<MessageVO> messages = new ArrayList<>();

    @GetMapping("/chat")
    public String chat(@RequestParam String userMessage, Model model) {
        // 获取当前时间
        String time = LocalTime.now().toString();

        // 添加用户消息
        messages.add(new MessageVO("user", userMessage, time));

        // 获取AI回复
        String reply = chatService.getMessage(userMessage);
        messages.add(new MessageVO("ai", reply, time));

        // 将消息列表传递给前端
        model.addAttribute("messages", messages);
        return "index";
    }
}
