package com.codeinterview.practice;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
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

import com.codeinterview.practice.dto.AssessmentReq;
import com.codeinterview.practice.vo.AssessmentResp;
import com.codeinterview.user.UserService;

/**
 * T18 回归：01 §8.3 错题状态机五条转移 + wrong_count / mastered_at 一致性（T09/T10）。
 *
 * <p>用例自建用户与发布态题目、自清数据，不依赖 seed 题目的状态（q1 仍是草稿）。
 */
@SpringBootTest
@ActiveProfiles("mock")
class AssessmentStateMachineTest {

    @Autowired
    private AssessmentService assessmentService;
    @Autowired
    private WrongBookService wrongBookService;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long questionId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        userId = userService.create("t18asm_" + suffix, "123456", 0).getId();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO question
                        (category_id, title, content_md, answer_md, difficulty, status, source, published_at, created_by)
                    VALUES (1, ?, '题干', '答案', 1, 1, 'T18 单测', NOW(3), 1)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "T18 状态机题 " + suffix);
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
        jdbcTemplate.update("DELETE FROM practice_record WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM wrong_record WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM question WHERE id = ?", questionId);
        jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", userId);
    }

    @Test
    void fiveTransitionsFollowStateMachine() {
        // 1) 无记录 + 自评「不会」→ 新建错题行
        AssessmentResp first = assess(3, 1);
        assertThat(first.getWrongBook()).isNotNull();
        assertThat(first.getWrongBook().getResolved()).isZero();
        assertWrongRecord(3, 1, 0, false);
        assertThat(practiceCount()).isEqualTo(1);
        assertThat(questionPracticeCount()).isEqualTo(1);

        // 2) 在错题本 + 自评「模糊」→ wrong_count+1
        assess(2, 1);
        assertWrongRecord(2, 2, 0, false);

        // 3) 在错题本 + 自评「掌握」→ 已掌握、mastered_at 非空、wrong_count 保留
        AssessmentResp mastered = assess(1, 1);
        assertThat(mastered.getWrongBook().getResolved()).isEqualTo(1);
        assertWrongRecord(1, 2, 1, true);
        assertThat(practiceCount()).isEqualTo(3);
        assertThat(questionPracticeCount()).isEqualTo(3);

        // 4) 已掌握 + 自评「不会」→ 回到错题本、清空 mastered_at、wrong_count+1
        assess(3, 1);
        assertWrongRecord(3, 3, 0, false);
        assertThat(practiceCount()).isEqualTo(4);

        // 5) 手动标记掌握 → 三列同写；再手动彻底移除 → 物理删除
        assertThat(wrongBookService.markResolved(userId, questionId)).isEqualTo(1);
        assertWrongRecord(1, 3, 1, true);
        wrongBookService.remove(userId, questionId);
        assertThat(wrongRecord()).isNull();
        assertThat(practiceCount()).isEqualTo(4);
        assertThat(questionPracticeCount()).isEqualTo(4);
    }

    @Test
    void masteredWithoutWrongRecordDoesNotCreateRow() {
        AssessmentResp resp = assess(1, 1);

        assertThat(resp.getWrongBook()).isNull();
        assertThat(wrongRecord()).isNull();
        assertThat(practiceCount()).isEqualTo(1);
    }

    private AssessmentResp assess(int selfAssess, int source) {
        AssessmentReq req = new AssessmentReq();
        req.setSelfAssess(selfAssess);
        req.setSource(source);
        return assessmentService.assess(userId, questionId, req);
    }

    private void assertWrongRecord(int lastAssess, int wrongCount, int resolved, boolean mastered) {
        Map<String, Object> row = wrongRecord();
        assertThat(row).isNotNull();
        assertThat(((Number) row.get("last_assess")).intValue()).isEqualTo(lastAssess);
        assertThat(((Number) row.get("wrong_count")).intValue()).isEqualTo(wrongCount);
        assertThat(((Number) row.get("resolved")).intValue()).isEqualTo(resolved);
        if (mastered) {
            assertThat(row.get("mastered_at")).isNotNull();
        } else {
            assertThat(row.get("mastered_at")).isNull();
        }
    }

    private Map<String, Object> wrongRecord() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT last_assess, wrong_count, resolved, mastered_at FROM wrong_record "
                        + "WHERE user_id = ? AND question_id = ?", userId, questionId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private int practiceCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM practice_record WHERE user_id = ? AND question_id = ?",
                Integer.class, userId, questionId);
        return count == null ? 0 : count;
    }

    private int questionPracticeCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT practice_count FROM question WHERE id = ?", Integer.class, questionId);
        return count == null ? 0 : count;
    }
}
