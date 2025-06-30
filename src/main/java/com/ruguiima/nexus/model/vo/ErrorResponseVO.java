package com.ruguiima.nexus.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseVO {
    private String error;
    private int status;
    private String message;
    
    public static ErrorResponseVO of(String message, int status) {
        return new ErrorResponseVO("error", status, message);
    }
}
