package com.ruguiima.nexus.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ruguiima.nexus.model.dto.UserLoginDTO;
import com.ruguiima.nexus.model.entity.User;
import com.ruguiima.nexus.service.SessionService;
import com.ruguiima.nexus.service.UserService;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private SessionService sessionService;

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        // 如果已经登录，重定向到首页
        if (sessionService.isLoggedIn(session)) {
            return "redirect:/";
        }
        
        model.addAttribute("loginDTO", new UserLoginDTO());
        return "login";
    }
    
    @PostMapping("/login")
    public String login(@ModelAttribute UserLoginDTO loginDTO, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.loginUser(loginDTO.getUsername(), loginDTO.getPassword());
            // 将用户信息存入会话
            sessionService.saveUserToSession(session, user);
            
            // 使用POST-Redirect-GET模式，防止刷新时重复提交
            return "redirect:/";
        } catch (Exception e) {
            // 登录失败
            redirectAttributes.addFlashAttribute("loginError", e.getMessage());
            return "redirect:/login";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        sessionService.logout(session);
        return "redirect:/login";
    }
}
