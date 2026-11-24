package com.codeinterview.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.note.dto.NoteSaveReq;
import com.codeinterview.note.vo.NoteResp;
import com.codeinterview.user.UserService;

/**
 * T23 回归：笔记保存/读取/清空删除/导出 Markdown（01 F-011 / 04 C5）。
 *
 * <p>用例自建用户与发布态题目、自清数据，不依赖 seed 数据的分类与题目状态。
 */
@SpringBootTest
@ActiveProfiles({"mock", "test"})
class NoteServiceTest {

    @Autowired
    private NoteService noteService;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long questionId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        userId = userService.create("t23note_" + suffix, "123456", 0).getId();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO question
                        (category_id, title, content_md, answer_md, difficulty, status, source, published_at, created_by)
                    VALUES (1, ?, '题干', '答案', 1, 1, 'T23 单测', NOW(3), 1)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "T23 笔记题 " + suffix);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("create question failed");
        }
        questionId = key.longValue();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM question_note WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM question WHERE id = ?", questionId);
        jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", userId);
    }

    @Test
    void saveThenGetRoundTrips() {
        NoteResp empty = noteService.get(userId, questionId);
        assertThat(empty.getContentMd()).isEmpty();
        assertThat(empty.getUpdatedAt()).isNull();

        NoteResp saved = noteService.save(userId, questionId, req("## 我的理解\n\n- 可变性"));
        assertThat(saved.getContentMd()).contains("我的理解");
        assertThat(saved.getUpdatedAt()).isNotNull();

        NoteResp loaded = noteService.get(userId, questionId);
        assertThat(loaded.getContentMd()).isEqualTo(saved.getContentMd());
        assertThat(noteRows()).isEqualTo(1);

        // 覆盖保存仍只有一行（唯一键 + upsert）
        noteService.save(userId, questionId, req("第二版"));
        assertThat(noteRows()).isEqualTo(1);
        assertThat(noteService.get(userId, questionId).getContentMd()).isEqualTo("第二版");
    }

    @Test
    void blankContentDeletesRow() {
        noteService.save(userId, questionId, req("先写一条"));
        assertThat(noteRows()).isEqualTo(1);

        NoteResp cleared = noteService.save(userId, questionId, req("   \n  "));
        assertThat(cleared.getContentMd()).isEmpty();
        assertThat(noteRows()).isZero();
    }

    @Test
    void overlongContentRejected() {
        String tooLong = "x".repeat(20_001);
        assertThatThrownBy(() -> noteService.save(userId, questionId, req(tooLong)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("20000");
    }

    @Test
    void exportMarkdownGroupsByCategory() {
        noteService.save(userId, questionId, req("导出用的笔记内容"));
        String markdown = noteService.exportMarkdown(userId);

        assertThat(markdown).startsWith("# 我的面试笔记");
        assertThat(markdown).contains("## Java");
        assertThat(markdown).contains("### T23 笔记题");
        assertThat(markdown).contains("导出用的笔记内容");
        assertThat(markdown).contains("共 1 条");
    }

    @Test
    void exportWithoutNotesIsStillAFile() {
        String markdown = noteService.exportMarkdown(userId);
        assertThat(markdown).contains("# 我的面试笔记");
        assertThat(markdown).contains("暂无笔记");
        assertThat(noteService.exportFileName()).endsWith(".md");
    }

    @Test
    void draftQuestionRejected() {
        jdbcTemplate.update("UPDATE question SET status = 0 WHERE id = ?", questionId);
        assertThatThrownBy(() -> noteService.save(userId, questionId, req("草稿题不该能写笔记")))
                .isInstanceOf(BusinessException.class);
    }

    private NoteSaveReq req(String content) {
        NoteSaveReq req = new NoteSaveReq();
        req.setContentMd(content);
        return req;
    }

    private int noteRows() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question_note WHERE user_id = ? AND question_id = ?",
                Integer.class, userId, questionId);
        return count == null ? 0 : count;
    }
}
