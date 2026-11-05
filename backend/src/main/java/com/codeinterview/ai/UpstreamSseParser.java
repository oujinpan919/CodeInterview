package com.codeinterview.ai;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上游 SSE data 行解析（05 §2.2 / §2.3 / §9.3）。
 *
 * <p>三条硬规则：
 * <ol>
 *   <li>讯飞每行都自带 code/message/sid，HTTP 200 也可能是错误：<b>先判 code</b>，
 *       code != 0 时该行不得进入 delta 累积；</li>
 *   <li>只读 choices[0].delta.content，<b>显式忽略 reasoning_content</b>（thinking 关闭后仍可能出现）；</li>
 *   <li>security_suggest.action == HIDE_CONTINUE 时跳过该段内容（返回 securityHide=true），
 *       由调用方继续读后续 delta，不中断整条流。</li>
 * </ol>
 * 解析异常/空行不抛出：返回空结果跳过该行，避免一行坏数据毁掉整条流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpstreamSseParser {

    private static final String HIDE_CONTINUE = "HIDE_CONTINUE";
    /** usage 缺失时的字符估算口径：中文为主，1 token ≈ 1.6 字符（05 §2.3）。 */
    private static final double CHARS_PER_TOKEN = 1.6;

    private final ObjectMapper objectMapper;

    public ParseResult parse(String payload) {
        if (payload == null || payload.isBlank()) {
            return ParseResult.empty();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (IOException ex) {
            // 不打印原始内容（可能含回答正文），只记录长度
            log.warn("[ai] skip malformed upstream SSE line, length={}", payload.length());
            return ParseResult.empty();
        }
        if (root == null || !root.isObject()) {
            return ParseResult.empty();
        }
        int code = root.path("code").asInt(0);
        String message = textOrNull(root.path("message"));
        String sid = textOrNull(root.path("sid"));
        if (code != 0) {
            return new ParseResult(code, message, sid, null, false, null, null);
        }

        JsonNode choice = firstChoice(root);
        JsonNode delta = choice == null ? null : choice.path("delta");
        String finishReason = choice == null ? null : textOrNull(choice.path("finish_reason"));
        String content = delta == null ? null : textOrNull(delta.path("content"));
        boolean securityHide = delta != null && isHideContinue(delta.path("security_suggest"));
        if (securityHide) {
            // 该段内容不转发、不追加，但后续 delta 继续收（05 §9.3）
            content = null;
        }
        Usage usage = parseUsage(root.path("usage"));
        return new ParseResult(code, message, sid, content, securityHide, usage, finishReason);
    }

    /** usage 缺失时按字符估算，estimated=1；不得因此中断流（05 §2.3）。 */
    public Usage estimateUsage(int promptChars, int completionChars) {
        int prompt = (int) Math.ceil(Math.max(0, promptChars) / CHARS_PER_TOKEN);
        int completion = (int) Math.ceil(Math.max(0, completionChars) / CHARS_PER_TOKEN);
        return new Usage(prompt, completion, prompt + completion, 1);
    }

    private JsonNode firstChoice(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode choice = choices.get(0);
        return choice != null && choice.isObject() ? choice : null;
    }

    private boolean isHideContinue(JsonNode securitySuggest) {
        return securitySuggest != null
                && securitySuggest.isObject()
                && HIDE_CONTINUE.equalsIgnoreCase(securitySuggest.path("action").asText(""));
    }

    private Usage parseUsage(JsonNode usageNode) {
        if (usageNode == null || !usageNode.isObject()) {
            return null;
        }
        int prompt = usageNode.path("prompt_tokens").asInt(0);
        // deep_search 才会出现该字段；出现则计入 prompt_tokens，保证 total 口径不缺账
        int searchPrompt = usageNode.path("search_prompt_tokens").asInt(0);
        int completion = usageNode.path("completion_tokens").asInt(0);
        int promptTotal = prompt + searchPrompt;
        int total = usageNode.path("total_tokens").asInt(promptTotal + completion);
        return new Usage(promptTotal, completion, total, 0);
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    /** 一次 parse 的结果；delta == null 表示该行没有可转发内容。 */
    public record ParseResult(int code,
                              String message,
                              String sid,
                              String delta,
                              boolean securityHide,
                              Usage usage,
                              String finishReason) {

        public static ParseResult empty() {
            return new ParseResult(0, null, null, null, false, null, null);
        }

        public boolean hasDelta() {
            return delta != null && !delta.isEmpty();
        }

        public boolean hasUsage() {
            return usage != null;
        }

        public boolean isError() {
            return code != 0;
        }
    }

    /** estimated=1 表示上游没给 usage、按字符估算。 */
    public record Usage(int promptTokens, int completionTokens, int totalTokens, int estimated) {
    }
}
