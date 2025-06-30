package com.ruguiima.AIGame.service;

import com.ruguiima.AIGame.model.entity.User;
import java.util.Optional;

public interface UserService {
    User registerUser(String username, String email, String password);
    User loginUser(String username, String password);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> getUserById(Long userId);
    Optional<User> getUserByUsername(String username);
    User updateUser(User user);
}
