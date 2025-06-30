package com.ruguiima.nexus.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ruguiima.nexus.model.dto.UserRegistrationDTO;
import com.ruguiima.nexus.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RegisterController {

    @Autowired
    private UserService userService;
    
    @PostMapping("/api/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO registrationDTO) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 输出接收到的注册信息，方便调试
            System.out.println("收到API注册请求: " + registrationDTO.getUsername() + ", " + registrationDTO.getEmail());
            
            // 基本验证
            if (registrationDTO.getUsername() == null || registrationDTO.getUsername().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }
            
            if (registrationDTO.getEmail() == null || registrationDTO.getEmail().isEmpty()) {
                throw new RuntimeException("邮箱不能为空");
            }
            
            if (registrationDTO.getPassword() == null || registrationDTO.getPassword().isEmpty()) {
                throw new RuntimeException("密码不能为空");
            }
            
            // 注册用户
            userService.registerUser(
                registrationDTO.getUsername(), 
                registrationDTO.getEmail(), 
                registrationDTO.getPassword()
            );
            
            // 注册成功
            response.put("success", true);
            response.put("message", "注册成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // 注册失败，打印详细错误
            System.err.println("API注册失败: " + e.getMessage());
            e.printStackTrace();
            
            // 返回错误信息
            response.put("success", false);
            response.put("message", "注册失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
