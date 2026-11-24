package com.codeinterview.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
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

import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.question.vo.NeighborIds;
import com.codeinterview.question.vo.NeighborResp;
import com.codeinterview.question.vo.QuestionDetailResp;
import com.codeinterview.user.UserService;

/**
 * T24 回归：上下题按来源列表上下文（v3.6 修1 / 01 F-012）。
 *
 * <p>用唯一关键词把测试题从 721 道既有题里隔离出来，避免列表顺序被其他题干扰。
 */
@SpringBootTest
@ActiveProfiles({"mock", "test"})
class QuestionNeighborContextTest {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long categoryId;
    private final List<Long> questionIds = new ArrayList<>();
    /** 三道题的标题统一前缀，同时作为列表筛选关键词 */
    private String keyword;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        userId = userService.create("t24nav_" + suffix, "123456", 0).getId();
        keyword = "T24nav" + suffix;

        // 自建启用分类：不能依赖既有 category_id=1（v7/v8 迁移后可能已停用/替换）
        jdbcTemplate.update(
                "INSERT INTO category (name, description, sort, status) VALUES (?, 'T24 上下文单测', 0, 1)",
                "T24分类" + suffix);
        categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = ?", Long.class, "T24分类" + suffix);

        // 依次创建 3 题：published_at 递减 → latest 顺序为 q0, q1, q2（q0 最新）
        for (int i = 0; i < 3; i++) {
            final int index = i;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO question
                            (category_id, title, content_md, answer_md, difficulty, status, source,
                             published_at, created_by)
                        VALUES (?, ?, '题干', '答案', 1, 1, 'T24 单测',
                                DATE_SUB(NOW(3), INTERVAL ? HOUR), 1)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, categoryId);
                statement.setString(2, keyword + " 第 " + index + " 题");
                statement.setInt(3, index);
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("create question failed");
            }
            questionIds.add(key.longValue());
        }
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM user_favorite WHERE user_id = ?", userId);
        for (Long id : questionIds) {
            jdbcTemplate.update("DELETE FROM question WHERE id = ?", id);
        }
        jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", userId);
        if (categoryId != null) {
            jdbcTemplate.update("DELETE FROM category WHERE id = ?", categoryId);
        }
    }

    @Test
    void listContextFollowsFilteredOrder() {
        // 按关键词 + latest：顺序 = q0（最新）→ q1 → q2
        QuestionQueryReq query = new QuestionQueryReq();
        query.setKeyword(keyword);
        query.setSort("latest");
        QuestionDetailResp middle = questionService.detail(questionIds.get(1), "list", null, query);
        NeighborResp neighbors = middle.getNeighbors();
        assertThat(neighbors.getPrevId()).isEqualTo(questionIds.get(0));
        assertThat(neighbors.getNextId()).isEqualTo(questionIds.get(2));

        // hot 排序下三题 practice_count 都是 0，退回 id DESC 次序：q2 → q1 → q0
        QuestionQueryReq hotQuery = new QuestionQueryReq();
        hotQuery.setKeyword(keyword);
        hotQuery.setSort("hot");
        NeighborResp hotNeighbors = questionService.detail(questionIds.get(1), "list", null, hotQuery)
                .getNeighbors();
        assertThat(hotNeighbors.getPrevId()).isEqualTo(questionIds.get(2));
        assertThat(hotNeighbors.getNextId()).isEqualTo(questionIds.get(0));
    }

    @Test
    void favoriteContextFollowsFavoriteOrder() {
        // 先收藏 q1、再收藏 q2 → 收藏列表顺序 = q2（最新收藏）→ q1
        favorite(questionIds.get(1));
        favorite(questionIds.get(2));

        // 直接测 Mapper：ctx=favorite 的取数依赖登录态，单测里没有 Web 上下文，绕开会话层只验 SQL
        NeighborIds fromQ2 = questionMapper.selectFavoriteNeighborIds(userId, questionIds.get(2));
        assertThat(fromQ2.getPrevId()).isNull();
        assertThat(fromQ2.getNextId()).isEqualTo(questionIds.get(1));

        NeighborIds fromQ1 = questionMapper.selectFavoriteNeighborIds(userId, questionIds.get(1));
        assertThat(fromQ1.getPrevId()).isEqualTo(questionIds.get(2));
        assertThat(fromQ1.getNextId()).isNull();
    }

    @Test
    void noContextKeepsCategoryOrder() {
        // 不传 ctx：同分类按 id 升序（v3.5 行为），三题 id 连续
        NeighborResp neighbors = questionService.detail(questionIds.get(1)).getNeighbors();
        assertThat(neighbors.getPrevId()).isEqualTo(questionIds.get(0));
        assertThat(neighbors.getNextId()).isEqualTo(questionIds.get(2));
    }

    /** v3.8：列表排序白名单 —— difficulty 接受、其它值回落 latest。 */
    @Test
    void difficultySortAcceptedAndUnknownFallsBack() {
        QuestionQueryReq difficulty = new QuestionQueryReq();
        difficulty.setSort("difficulty");
        assertThat(questionService.normalize(difficulty).getSort()).isEqualTo("difficulty");

        QuestionQueryReq hot = new QuestionQueryReq();
        hot.setSort("hot");
        assertThat(questionService.normalize(hot).getSort()).isEqualTo("hot");

        QuestionQueryReq unknown = new QuestionQueryReq();
        unknown.setSort("oops");
        assertThat(questionService.normalize(unknown).getSort()).isEqualTo("latest");
    }

    private void favorite(Long questionId) {
        jdbcTemplate.update(
                "INSERT IGNORE INTO user_favorite (user_id, question_id, created_at) VALUES (?, ?, NOW(3))",
                userId, questionId);
    }
}
