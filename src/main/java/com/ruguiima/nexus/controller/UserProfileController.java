package com.ruguiima.nexus.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ruguiima.nexus.model.dto.UserProfileUpdateRequest;
import com.ruguiima.nexus.model.entity.User;
import com.ruguiima.nexus.model.vo.ErrorResponseVO;
import com.ruguiima.nexus.model.vo.UserProfileVO;
import com.ruguiima.nexus.service.SessionService;
import com.ruguiima.nexus.service.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserProfileController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private SessionService sessionService;
    
    // 上传目录配置
    private static final String UPLOAD_DIR = "uploads/avatars/";
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return ResponseEntity.status(401).body(ErrorResponseVO.of("未登录", 401));
        }
        
        User currentUser = sessionService.getCurrentUser(httpSession);
        UserProfileVO profile = UserProfileVO.fromUser(currentUser);
        
        return ResponseEntity.ok(profile);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserProfileUpdateRequest request, 
                                              HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return ResponseEntity.status(401).body(ErrorResponseVO.of("未登录", 401));
        }
        
        try {
            User currentUser = sessionService.getCurrentUser(httpSession);
            
            // 更新用户信息
            currentUser.setNickname(request.getNickname());
            currentUser.setGender(request.getGender());
            currentUser.setBirthday(request.getBirthday());
            
            // 保存到数据库
            User updatedUser = userService.updateUser(currentUser);
            
            // 更新session中的用户信息
            sessionService.setCurrentUser(httpSession, updatedUser);
            
            UserProfileVO profile = UserProfileVO.fromUser(updatedUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户信息更新成功");
            response.put("profile", profile);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return ResponseEntity.status(500).body(ErrorResponseVO.of("更新用户信息失败: " + e.getMessage(), 500));
        }
    }
    
    /**
     * 上传用户头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file,
                                        HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return ResponseEntity.status(401).body(ErrorResponseVO.of("未登录", 401));
        }
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ErrorResponseVO.of("请选择一个文件", 400));
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(ErrorResponseVO.of("只能上传图片文件", 400));
        }
        
        // 检查文件大小（限制为5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(ErrorResponseVO.of("文件大小不能超过5MB", 400));
        }
        
        try {
            User currentUser = sessionService.getCurrentUser(httpSession);
            
            // 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = "avatar_" + currentUser.getId() + "_" + UUID.randomUUID().toString() + fileExtension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 更新用户头像URL
            String avatarUrl = "/uploads/avatars/" + fileName;
            currentUser.setAvatarUrl(avatarUrl);
            User updatedUser = userService.updateUser(currentUser);
            
            // 更新session中的用户信息
            sessionService.setCurrentUser(httpSession, updatedUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "头像上传成功");
            response.put("avatarUrl", avatarUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("头像上传失败", e);
            return ResponseEntity.status(500).body(ErrorResponseVO.of("头像上传失败: " + e.getMessage(), 500));
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return ResponseEntity.status(500).body(ErrorResponseVO.of("头像上传失败: " + e.getMessage(), 500));
        }
    }
}
