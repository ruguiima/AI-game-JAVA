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
        model.addAttribute("registrationDTO", new UserRegistrationDTO());
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
    
    @PostMapping("/register")
    public String register(@ModelAttribute UserRegistrationDTO registrationDTO, Model model) {
        try {
            // 输出接收到的注册信息，方便调试
            System.out.println("收到注册请求: " + registrationDTO.getUsername() + ", " + registrationDTO.getEmail());
            
            if (registrationDTO.getUsername() == null || registrationDTO.getUsername().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }
            
            if (registrationDTO.getEmail() == null || registrationDTO.getEmail().isEmpty()) {
                throw new RuntimeException("邮箱不能为空");
            }
            
            if (registrationDTO.getPassword() == null || registrationDTO.getPassword().isEmpty()) {
                throw new RuntimeException("密码不能为空");
            }
            
            userService.registerUser(
                registrationDTO.getUsername(), 
                registrationDTO.getEmail(), 
                registrationDTO.getPassword()
            );
            
            // 注册成功，返回成功状态
            model.addAttribute("registrationSuccess", "注册成功");
            model.addAttribute("loginDTO", new UserLoginDTO());
            model.addAttribute("registrationDTO", new UserRegistrationDTO());
            return "login";
        } catch (Exception e) {
            // 注册失败，打印详细错误
            System.err.println("注册失败: " + e.getMessage());
            e.printStackTrace();
            
            // 将详细错误信息传递给前端
            model.addAttribute("registrationError", "注册失败: " + e.getMessage());
            model.addAttribute("loginDTO", new UserLoginDTO());
            model.addAttribute("registrationDTO", registrationDTO); // 保留用户输入
            return "login";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        sessionService.logout(session);
        return "redirect:/login";
    }
}
