package com.ruguiima.AIGame.controller;

import com.ruguiima.AIGame.model.dto.UserLoginDTO;
import com.ruguiima.AIGame.model.dto.UserRegistrationDTO;
import com.ruguiima.AIGame.model.entity.User;
import com.ruguiima.AIGame.service.SessionService;
import com.ruguiima.AIGame.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

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
        model.addAttribute("registrationDTO", new UserRegistrationDTO());
        return "login";
    }
    
    @PostMapping("/login")
    public String login(@ModelAttribute UserLoginDTO loginDTO, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.loginUser(loginDTO.getUsername(), loginDTO.getPassword());
            // 将用户信息存入会话
            sessionService.saveUserToSession(session, user);
            return "redirect:/";
        } catch (Exception e) {
            // 登录失败
            redirectAttributes.addFlashAttribute("loginError", e.getMessage());
            return "redirect:/login";
        }
    }
    
    @PostMapping("/register")
    public String register(@ModelAttribute UserRegistrationDTO registrationDTO, RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(
                registrationDTO.getUsername(), 
                registrationDTO.getEmail(), 
                registrationDTO.getPassword()
            );
            // 注册成功，显示成功消息
            redirectAttributes.addFlashAttribute("registrationSuccess", "注册成功，请登录");
            return "redirect:/login";
        } catch (Exception e) {
            // 注册失败
            redirectAttributes.addFlashAttribute("registrationError", e.getMessage());
            return "redirect:/login";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        sessionService.logout(session);
        return "redirect:/login";
    }
}
