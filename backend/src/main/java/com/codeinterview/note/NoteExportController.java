package com.codeinterview.note;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 笔记导出（04 C5 #49）。这是本系统第二个**不包 Result** 的接口（另一个是 /health），
 * 因为它要返回文件字节流；失败时仍走全局异常处理返回 Result。
 */
@Tag(name = "数据导出")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NoteExportController {

    private final NoteService noteService;

    @Operation(summary = "导出我的笔记为单个 Markdown 文件（Content-Disposition: attachment）")
    @GetMapping("/export/notes")
    public ResponseEntity<byte[]> exportNotes() {
        Long userId = StpUtil.getLoginIdAsLong();
        String markdown = noteService.exportMarkdown(userId);
        byte[] body = markdown.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "markdown", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(noteService.exportFileName())
                .build());
        headers.setCacheControl("no-store");
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
