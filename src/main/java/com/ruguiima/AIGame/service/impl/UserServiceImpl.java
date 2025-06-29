package com.ruguiima.AIGame.service.impl;

import com.ruguiima.AIGame.model.entity.User;
import com.ruguiima.AIGame.repository.UserRepository;
import com.ruguiima.AIGame.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User registerUser(String username, String email, String password) {
        // 检查用户名和邮箱是否已存在
        if (existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        
        if (existsByEmail(email)) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        // 在实际应用中，应该对密码进行加密处理
        user.setPassword(password);  
        user.setCreatedTime(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    @Override
    public User loginUser(String username, String password) {
        // 根据用户名查找用户
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        
        User user = userOptional.get();
        // 在实际应用中，应该对密码进行匹配而不是直接比较
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
