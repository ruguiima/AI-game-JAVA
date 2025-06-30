package com.ruguiima.nexus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruguiima.nexus.model.entity.User;
import com.ruguiima.nexus.repository.UserRepository;

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
    public User loginUser(String usernameOrEmail, String password) {
        User user = null;
        
        // 判断输入的是邮箱还是用户名（简单判断：包含@符号的认为是邮箱）
        if (usernameOrEmail.contains("@")) {
            // 按邮箱查找
            Optional<User> userOptional = userRepository.findByEmail(usernameOrEmail);
            if (userOptional.isEmpty()) {
                throw new RuntimeException("邮箱不存在");
            }
            user = userOptional.get();
        } else {
            // 按用户名查找
            Optional<User> userOptional = userRepository.findByUsername(usernameOrEmail);
            if (userOptional.isEmpty()) {
                throw new RuntimeException("用户名不存在");
            }
            user = userOptional.get();
        }
        
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

    @Override
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
