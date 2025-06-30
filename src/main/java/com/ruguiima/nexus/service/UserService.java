package com.ruguiima.nexus.service;

import java.util.Optional;

import com.ruguiima.nexus.model.entity.User;

public interface UserService {
    User registerUser(String username, String email, String password);
    User loginUser(String usernameOrEmail, String password);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> getUserById(Long userId);
    Optional<User> getUserByUsername(String username);
    Optional<User> getUserByEmail(String email);
    User updateUser(User user);
}
