package com.codeinterview.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
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

import com.codeinterview.common.result.PageResp;
import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.question.vo.QuestionBriefResp;
import com.codeinterview.user.UserService;

/**
 * T31 回归：关键词搜索的召回范围与相关性排序（v3.15 / 01 F-023）。
 *
 * <p>用唯一 token 隔离数据：标题命中 / 标签命中 / 仅答案命中 / 草稿 各一道，
 * 默认搜索只应召回标题与标签命中的题；开启 searchBody 才召回仅正文命中的题；排序按 4/3/1 权重递减。
 */
@SpringBootTest
@ActiveProfiles({"mock", "test"})
class QuestionSearchTest {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long categoryId;
    private Long tagId;
    private String token;
    private Long titleHitId;
    private Long tagHitId;
    private Long bodyOnlyId;
    private Long draftId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        token = "ZQX" + suffix;
        userId = userService.create("t31search_" + suffix.toLowerCase(), "123456", 0).getId();

        // 自建启用分类：不能依赖既有 category_id=1（v7/v8 迁移后可能已停用/替换）
        jdbcTemplate.update(
                "INSERT INTO category (name, description, sort, status) VALUES (?, 'T31 搜索单测', 0, 1)",
                "T31分类" + token);
        categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = ?", Long.class, "T31分类" + token);

        titleHitId = createQuestion(token + " 标题命中题", "题干", "答案", 1);
        tagHitId = createQuestion("标签命中题（标题不含 token）", "题干", "答案", 1);
        bodyOnlyId = createQuestion("仅正文命中题", "题干", "答案里提到 " + token, 1);
        draftId = createQuestion(token + " 草稿题", "题干", "答案", 0);

        jdbcTemplate.update("INSERT INTO tag (name) VALUES (?)", "标签" + token);
        tagId = jdbcTemplate.queryForObject("SELECT id FROM tag WHERE name = ?", Long.class, "标签" + token);
        // 只把标签挂给「标签命中题」：正文命中题的标签也必须不含 token，否则会混入标签召回
        jdbcTemplate.update("INSERT INTO question_tag (question_id, tag_id) VALUES (?, ?)", tagHitId, tagId);
    }

    @AfterEach
    void tearDown() {
        for (Long id : List.of(titleHitId, tagHitId, bodyOnlyId, draftId)) {
            if (id != null) {
                jdbcTemplate.update("DELETE FROM question_tag WHERE question_id = ?", id);
                jdbcTemplate.update("DELETE FROM question WHERE id = ?", id);
            }
        }
        if (tagId != null) {
            jdbcTemplate.update("DELETE FROM tag WHERE id = ?", tagId);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", userId);
        }
        if (categoryId != null) {
            jdbcTemplate.update("DELETE FROM category WHERE id = ?", categoryId);
        }
    }

    /** 默认（searchBody 缺省）：只召回标题与标签命中的题，草稿与"仅正文命中"都不出现。 */
    @Test
    void defaultSearchOnlyMatchesTitleAndTag() {
        PageResp<QuestionBriefResp> page = search(false);
        List<Long> ids = page.getList().stream().map(QuestionBriefResp::getId).toList();

        assertThat(ids).contains(titleHitId, tagHitId);
        assertThat(ids).doesNotContain(bodyOnlyId, draftId);
    }

    /** 开启 searchBody：仅正文命中的题也召回。 */
    @Test
    void deepSearchAlsoMatchesBody() {
        PageResp<QuestionBriefResp> page = search(true);
        List<Long> ids = page.getList().stream().map(QuestionBriefResp::getId).toList();

        assertThat(ids).contains(titleHitId, tagHitId, bodyOnlyId);
        assertThat(ids).doesNotContain(draftId);
    }

    /** 相关性排序：标题命中(4) > 标签命中(3) > 仅正文命中(1)。 */
    @Test
    void resultsOrderedByRelevance() {
        List<QuestionBriefResp> list = search(true).getList();
        List<Long> ids = list.stream().map(QuestionBriefResp::getId).toList();

        assertThat(ids.indexOf(titleHitId)).isLessThan(ids.indexOf(tagHitId));
        assertThat(ids.indexOf(tagHitId)).isLessThan(ids.indexOf(bodyOnlyId));
        assertThat(list.get(0).getRelevance()).isEqualTo(4);
    }

    /** 命中位置标记：前端据此显示「命中：标题 / 标签 / 答案」。 */
    @Test
    void matchFieldsDescribeWhereHit() {
        List<QuestionBriefResp> list = search(true).getList();

        assertThat(find(list, titleHitId).getMatchFields()).contains("title");
        assertThat(find(list, tagHitId).getMatchFields()).contains("tag");
        assertThat(find(list, bodyOnlyId).getMatchFields()).contains("body");
    }

    private QuestionBriefResp find(List<QuestionBriefResp> list, Long id) {
        return list.stream().filter(item -> id.equals(item.getId())).findFirst().orElseThrow();
    }

    private PageResp<QuestionBriefResp> search(boolean searchBody) {
        QuestionQueryReq query = new QuestionQueryReq();
        query.setKeyword(token);
        query.setSearchBody(searchBody);
        query.setPage(1);
        query.setPageSize(20);
        return questionService.page(query);
    }

    private Long createQuestion(String title, String content, String answer, int status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO question
                        (category_id, title, content_md, answer_md, difficulty, status, source,
                         published_at, created_by)
                    VALUES (?, ?, ?, ?, 1, ?, 'T31 单测', NOW(3), 1)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, categoryId);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.setString(4, answer);
            statement.setInt(5, status);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("create question failed");
        }
        return key.longValue();
    }
}
