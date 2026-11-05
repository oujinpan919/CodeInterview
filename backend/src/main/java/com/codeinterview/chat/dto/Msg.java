package com.codeinterview.chat.dto;

import lombok.Data;

/**
 * 客户端回传的一条对话消息（05 §3.2）。服务端不持久化会话正文，
 * role 只允许 user / assistant（system 一律丢弃，由服务端自己拼）。
 */
@Data
public class Msg {

    private String role;
    private String content;
}
