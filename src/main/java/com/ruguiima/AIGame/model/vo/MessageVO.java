package com.ruguiima.AIGame.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {
    private String sender; // 发送者（user 或 ai）
    private String content; // 消息内容
    private String time;    // 消息时间
}
