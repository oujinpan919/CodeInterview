package com.codeinterview.note;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.note.dto.NoteSaveReq;
import com.codeinterview.note.vo.NoteResp;
import com.codeinterview.note.vo.QuestionNoteRow;
import com.codeinterview.question.QuestionMapper;

import lombok.RequiredArgsConstructor;

/**
 * 题目私有笔记（01 F-011 / 03 §12）。
 *
 * <p>只服务本人；题目必须是发布态（草稿/下架 40400，与详情口径一致）。
 * 内容空白 = 删除该行，避免「空笔记」在导出物里变成一堆空标题。
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int MAX_CONTENT_LENGTH = 20_000;
    private static final DateTimeFormatter EXPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final NoteMapper noteMapper;
    private final QuestionMapper questionMapper;

    public NoteResp get(Long userId, Long questionId) {
        ensurePublished(questionId);
        NoteResp note = noteMapper.select(userId, questionId);
        if (note != null) {
            return note;
        }
        NoteResp empty = new NoteResp();
        empty.setContentMd("");
        return empty;
    }

    @Transactional
    public NoteResp save(Long userId, Long questionId, NoteSaveReq req) {
        ensurePublished(questionId);
        String content = req == null || req.getContentMd() == null ? "" : req.getContentMd();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "笔记最多 20000 字符");
        }
        LocalDateTime now = LocalDateTime.now(SHANGHAI);
        NoteResp resp = new NoteResp();
        if (content.isBlank()) {
            noteMapper.delete(userId, questionId);
            resp.setContentMd("");
            return resp;
        }
        noteMapper.upsert(userId, questionId, content, now);
        resp.setContentMd(content);
        resp.setUpdatedAt(now);
        return resp;
    }

    /** 导出为单文件 Markdown（C7）：按分类分组，已逻辑删除的题跳过。 */
    public String exportMarkdown(Long userId) {
        List<QuestionNoteRow> rows = noteMapper.selectForExport(userId);
        StringBuilder sb = new StringBuilder(1024);
        sb.append("# 我的面试笔记\n\n");
        sb.append("> 导出时间：").append(LocalDateTime.now(SHANGHAI).format(EXPORT_TIME))
                .append(" · 共 ").append(rows.size()).append(" 条\n\n");
        if (rows.isEmpty()) {
            sb.append("_暂无笔记。去题目详情页写下第一条吧。_\n");
            return sb.toString();
        }
        String currentCategory = null;
        for (QuestionNoteRow row : rows) {
            if (!row.getCategoryName().equals(currentCategory)) {
                currentCategory = row.getCategoryName();
                sb.append("## ").append(currentCategory).append("\n\n");
            }
            sb.append("### ").append(row.getTitle()).append("\n\n");
            if (row.getUpdatedAt() != null) {
                sb.append("_最后更新：").append(row.getUpdatedAt().format(EXPORT_TIME)).append("_\n\n");
            }
            sb.append(row.getContentMd()).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    /** 下载文件名固定 ASCII，避免不同浏览器对中文文件名的编码差异。 */
    public String exportFileName() {
        return "notes-" + LocalDateTime.now(SHANGHAI).format(FILE_TIME) + ".md";
    }

    private void ensurePublished(Long questionId) {
        if (questionMapper.countPublished(questionId) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }
}
