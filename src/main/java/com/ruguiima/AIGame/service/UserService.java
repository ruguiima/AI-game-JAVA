package com.ruguiima.AIGame.service;

import com.ruguiima.AIGame.model.entity.User;

public interface UserService {
    User registerUser(String username, String email, String password);
    User loginUser(String username, String password);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
