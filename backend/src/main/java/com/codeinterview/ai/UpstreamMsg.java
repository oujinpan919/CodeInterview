package com.codeinterview.ai;

/** 发给上游的一条消息（OpenAI 兼容格式）。 */
public record UpstreamMsg(String role, String content) {
}
