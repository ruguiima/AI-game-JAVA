package com.ruguiima.AIGame.controller;

import com.ruguiima.AIGame.model.dto.ModelSettingsDTO;
import com.ruguiima.AIGame.model.entity.User;
import com.ruguiima.AIGame.service.SessionService;
import com.ruguiima.AIGame.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/model-settings")
@RequiredArgsConstructor
@Slf4j
public class ModelSettingsController {

    private final UserService userService;
    private final SessionService sessionService;

    /**
     * 获取用户的模型设置
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserModelSettings(HttpSession session) {
        try {
            // 使用SessionService检查登录状态
            if (!sessionService.isLoggedIn(session)) {
                log.warn("用户未登录，无法获取模型设置");
                return ResponseEntity.badRequest().body(Map.of("error", "用户未登录"));
            }

            User user = sessionService.getCurrentUser(session);
            if (user == null) {
                log.warn("无法从会话中获取用户信息");
                return ResponseEntity.badRequest().body(Map.of("error", "用户会话已过期"));
            }

            log.info("获取用户模型设置请求，用户ID: {}", user.getId());

            // 构建响应数据，处理null值
            Map<String, Object> settings = new HashMap<>();
            settings.put("model", user.getPreferredModel() != null ? user.getPreferredModel() : "deepseek-chat");
            settings.put("responseLength", user.getResponseLength() != null ? user.getResponseLength() : "short");
            settings.put("creativity", user.getCreativityLevel() != null ? user.getCreativityLevel() : "precise");
            settings.put("maxTokens", user.getMaxTokens() != null ? user.getMaxTokens() : 500);
            settings.put("temperature", user.getTemperature() != null ? user.getTemperature() : 0.2);

            log.info("返回用户模型设置: {}", settings);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("获取模型设置失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "获取设置失败"));
        }
    }

    /**
     * 更新用户的模型设置
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> updateModelSettings(
            @RequestBody ModelSettingsDTO settingsDTO,
            HttpSession session) {
        try {
            log.info("收到模型设置更新请求: {}", settingsDTO);
            
            // 使用SessionService检查登录状态
            if (!sessionService.isLoggedIn(session)) {
                log.warn("用户未登录，无法更新模型设置");
                return ResponseEntity.badRequest().body(Map.of("error", "用户未登录"));
            }

            User user = sessionService.getCurrentUser(session);
            if (user == null) {
                log.warn("无法从会话中获取用户信息");
                return ResponseEntity.badRequest().body(Map.of("error", "用户会话已过期"));
            }

            log.info("用户 {} 请求更新模型设置", user.getId());

            // 验证和设置模型设置
            if (settingsDTO.getModel() != null) {
                if (!isValidModel(settingsDTO.getModel())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "无效的模型类型"));
                }
                user.setPreferredModel(settingsDTO.getModel());
            }

            if (settingsDTO.getResponseLength() != null) {
                if (!isValidResponseLength(settingsDTO.getResponseLength())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "无效的回复长度"));
                }
                user.setResponseLength(settingsDTO.getResponseLength());
                // 根据回复长度设置maxTokens
                user.setMaxTokens(getMaxTokensByLength(settingsDTO.getResponseLength()));
            }

            if (settingsDTO.getCreativity() != null) {
                if (!isValidCreativity(settingsDTO.getCreativity())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "无效的创意倾向"));
                }
                user.setCreativityLevel(settingsDTO.getCreativity());
                // 根据创意倾向设置temperature
                user.setTemperature(getTemperatureByCreativity(settingsDTO.getCreativity()));
            }

            // 保存更新
            User savedUser = userService.updateUser(user);
            log.info("用户 {} 的模型设置已更新", user.getId());

            // 返回更新后的设置，确保不包含null值
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "设置已保存");
            result.put("settings", Map.of(
                "model", savedUser.getPreferredModel() != null ? savedUser.getPreferredModel() : "deepseek-chat",
                "responseLength", savedUser.getResponseLength() != null ? savedUser.getResponseLength() : "short",
                "creativity", savedUser.getCreativityLevel() != null ? savedUser.getCreativityLevel() : "precise",
                "maxTokens", savedUser.getMaxTokens() != null ? savedUser.getMaxTokens() : 500,
                "temperature", savedUser.getTemperature() != null ? savedUser.getTemperature() : 0.2
            ));

            log.info("返回更新结果: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("更新模型设置失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "保存设置失败"));
        }
    }

    private boolean isValidModel(String model) {
        return "deepseek-chat".equals(model) || "deepseek-reasoner".equals(model);
    }

    private boolean isValidResponseLength(String length) {
        return "short".equals(length) || "medium".equals(length) || "long".equals(length);
    }

    private boolean isValidCreativity(String creativity) {
        return "precise".equals(creativity) || "balanced".equals(creativity) || "creative".equals(creativity);
    }

    private Integer getMaxTokensByLength(String length) {
        return switch (length) {
            case "short" -> 500;
            case "medium" -> 1000;
            case "long" -> 2000;
            default -> 500;
        };
    }

    private Double getTemperatureByCreativity(String creativity) {
        return switch (creativity) {
            case "precise" -> 0.2;
            case "balanced" -> 0.5;
            case "creative" -> 0.8;
            default -> 0.2;
        };
    }
}
