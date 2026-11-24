package com.codeinterview.ai.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * Prompt 模板（D11 / migration-v5 §5）。用户只能传 templateId，正文由服务端查库。
 */
@Data
@TableName("ai_prompt_template")
public class AiPromptTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String content;
    private String description;
    private Integer isBuiltin;
    private Integer enabled;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
