package com.ruguiima.AIGame.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用户信息更新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    private String nickname;
    private String gender;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
}
