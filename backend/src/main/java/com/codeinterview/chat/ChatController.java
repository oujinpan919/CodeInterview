package com.codeinterview.chat;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.codeinterview.chat.dto.ChatReq;
import com.codeinterview.chat.vo.AiQuotaResp;
import com.codeinterview.common.result.Result;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 助手入口（04 A3.7 #24/#25）。userId 只在 Controller 取一次，worker 线程内禁止 StpUtil。
 */
@Tag(name = "AI 助手")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final QuotaService quotaService;

    @Operation(summary = "助手额度（账号日额度 / 全站剩余）")
    @GetMapping("/quota")
    public Result<AiQuotaResp> quota() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(quotaService.quotaInfo(userId));
    }

    /**
     * 流开始前失败（40000/40100/40400/42901/42902/50201）由 GlobalExceptionHandler 返回 JSON；
     * 流开始后只发 event:error（05 §4.3）。
     */
    @Operation(summary = "助手流式对话（SSE，05 §4 契约）")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatReq req,
                           @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                           HttpServletResponse response) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (sessionId != null && !sessionId.isBlank()) {
            req.setSessionId(sessionId.trim());
        }
        SseEmitter emitter = chatService.chat(userId, req);
        // 建流成功后才写 SSE 响应头：流前异常必须保持 application/json
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        return emitter;
    }
}
