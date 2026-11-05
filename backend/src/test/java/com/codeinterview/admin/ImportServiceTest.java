package com.codeinterview.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.codeinterview.admin.imports.ImportResult;
import com.codeinterview.admin.imports.ImportService;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * T18 回归：JSON 批量导入的三种分支（03 §7.3 / 06 T13 验收）。
 * <ul>
 *   <li>20 题含 2 条重复 → created=18, skipped=2；</li>
 *   <li>单条缺 answer_md → 该条 failed 且 index 可定位，其余成功；</li>
 *   <li>51 题 → 整批 40000，不入库；非法 JSON → 40000。</li>
 * </ul>
 * 每个用例自建唯一分类/标签并自清，不依赖执行顺序与库内残留数据。
 */
@SpringBootTest
@ActiveProfiles("mock")
class ImportServiceTest {

    @Autowired
    private ImportService importService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private String categoryName;
    private String tagName;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        categoryName = "T18导入分类" + suffix;
        tagName = "T18标签" + suffix;
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("""
                DELETE qt FROM question_tag qt
                JOIN question q ON q.id = qt.question_id
                WHERE q.category_id IN (SELECT id FROM category WHERE name = ?)
                """, categoryName);
        jdbcTemplate.update(
                "DELETE FROM question WHERE category_id IN (SELECT id FROM category WHERE name = ?)", categoryName);
        jdbcTemplate.update("DELETE FROM tag WHERE name = ?", tagName);
        jdbcTemplate.update("DELETE FROM category WHERE name = ?", categoryName);
    }

    @Test
    void twentyQuestionsWithTwoDuplicatesAreCreatedAndSkipped() throws Exception {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            questions.add(question("T18导入题" + i, "题干 " + i, "答案 " + i, 2));
        }
        // 两条与已有题完全重复（同分类 + 同 title + 同 content_md）→ skipped
        questions.add(question("T18导入题0", "题干 0", "答案 0", 2));
        questions.add(question("T18导入题1", "题干 1", "答案 1", 2));

        ImportResult result = importService.importJson(1L, importJson(questions));

        assertThat(result.getCreated()).isEqualTo(18);
        assertThat(result.getSkipped()).isEqualTo(2);
        assertThat(result.getFailed()).isZero();
        assertThat(result.getErrors()).isEmpty();
        assertThat(questionCountByCategory()).isEqualTo(18);
        assertThat(questionTagCount()).isEqualTo(18);
        assertThat(tagExists()).isTrue();
    }

    @Test
    void oneItemMissingAnswerFailsAloneWithIndex() throws Exception {
        List<Map<String, Object>> questions = new ArrayList<>();
        questions.add(question("T18导入题A", "题干 A", "答案 A", 1));
        Map<String, Object> missingAnswer = new LinkedHashMap<>();
        missingAnswer.put("title", "T18导入题B");
        missingAnswer.put("content_md", "题干 B");
        missingAnswer.put("difficulty", 2);
        questions.add(missingAnswer);
        questions.add(question("T18导入题C", "题干 C", "答案 C", 3));

        ImportResult result = importService.importJson(1L, importJson(questions));

        assertThat(result.getCreated()).isEqualTo(2);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getIndex()).isEqualTo(1);
        assertThat(result.getErrors().get(0).getReason()).contains("answer_md");
        assertThat(questionCountByCategory()).isEqualTo(2);
    }

    @Test
    void fiftyOneQuestionsAreRejectedWithoutWriting() throws Exception {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            questions.add(question("T18导入题" + i, "题干 " + i, "答案 " + i, 1));
        }
        String json = importJson(questions);

        assertThatThrownBy(() -> importService.importJson(1L, json))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getResultCode()).isEqualTo(ResultCode.PARAM_ERROR);
                    assertThat(ex.getMessage()).contains("1–50");
                });
        assertThat(questionCountByCategory()).isZero();
        assertThat(categoryExists()).isFalse();
    }

    @Test
    void invalidJsonIsRejectedAsParamError() {
        assertThatThrownBy(() -> importService.importJson(1L, "{not-a-json"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getResultCode()).isEqualTo(ResultCode.PARAM_ERROR));
    }

    private String importJson(List<Map<String, Object>> questions) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("category", categoryName);
        root.put("tags", List.of(tagName));
        root.put("questions", questions);
        return objectMapper.writeValueAsString(root);
    }

    private Map<String, Object> question(String title, String content, String answer, int difficulty) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("title", title);
        question.put("content_md", content);
        question.put("answer_md", answer);
        question.put("difficulty", difficulty);
        return question;
    }

    private int questionCountByCategory() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question WHERE category_id IN (SELECT id FROM category WHERE name = ?)",
                Integer.class, categoryName);
        return count == null ? 0 : count;
    }

    private int questionTagCount() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM question_tag qt
                JOIN question q ON q.id = qt.question_id
                WHERE q.category_id IN (SELECT id FROM category WHERE name = ?)
                """, Integer.class, categoryName);
        return count == null ? 0 : count;
    }

    private boolean tagExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tag WHERE name = ?", Integer.class, tagName);
        return count != null && count > 0;
    }

    private boolean categoryExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category WHERE name = ?", Integer.class, categoryName);
        return count != null && count > 0;
    }
}
