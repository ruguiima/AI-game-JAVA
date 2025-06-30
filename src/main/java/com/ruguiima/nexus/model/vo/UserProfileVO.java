package com.ruguiima.nexus.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用户信息响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {
    private String username;
    private String email;
    private String nickname;
    private String gender;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    private String avatarUrl;
    
    public static UserProfileVO fromUser(com.ruguiima.nexus.model.entity.User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday());
        vo.setAvatarUrl(user.getAvatarUrl());
        return vo;
    }
}
