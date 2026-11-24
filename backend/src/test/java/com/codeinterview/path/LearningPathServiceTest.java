package com.codeinterview.path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.codeinterview.path.vo.PathDetailResp;
import com.codeinterview.path.vo.PathItemResp;
import com.codeinterview.path.vo.PathResp;
import com.codeinterview.user.UserService;

/**
 * T26 回归：刷题路线列表/详情/进度（01 F-017 / 04 C8）。
 *
 * <p>路线内容来自 `db/migration-v4-path.sql`；如果库里还没跑迁移，用例用 assumption 跳过而不是误报失败。
 */
@SpringBootTest
@ActiveProfiles({"mock", "test"})
class LearningPathServiceTest {

    @Autowired
    private LearningPathService learningPathService;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @AfterEach
    void tearDown() {
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM user_favorite WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM practice_record WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", userId);
            userId = null;
        }
    }

    @Test
    void guestListReturnsPathsWithoutProgress() {
        List<PathResp> paths = learningPathService.list(null);
        assumeThat(paths).as("需要先执行 db/migration-v4-path.sql").isNotEmpty();

        PathResp first = paths.get(0);
        assertThat(first.getName()).isNotBlank();
        assertThat(first.getTotalCount()).isPositive();
        // 游客不返回进度，前端据此显示「共 N 题」
        assertThat(first.getCompletedCount()).isNull();
    }

    @Test
    void detailReturnsOrderedItemsForGuest() {
        List<PathResp> paths = learningPathService.list(null);
        assumeThat(paths).as("需要先执行 db/migration-v4-path.sql").isNotEmpty();

        PathDetailResp detail = learningPathService.detail(paths.get(0).getId(), null);
        assertThat(detail.getItems()).isNotEmpty();
        assertThat(detail.getCompletedCount()).isNull();
        assertThat(detail.getTotalCount()).isEqualTo(detail.getItems().size());
        // 序号从 1 开始且单调递增（由易到难）
        int expected = 1;
        for (PathItemResp item : detail.getItems()) {
            assertThat(item.getSort()).isEqualTo(expected++);
        }
        assertThat(detail.getItems()).allSatisfy(item -> assertThat(item.getDone()).isFalse());
    }

    @Test
    void completedCountFollowsPracticeRecords() {
        List<PathResp> paths = learningPathService.list(null);
        assumeThat(paths).as("需要先执行 db/migration-v4-path.sql").isNotEmpty();
        PathResp first = paths.get(0);
        PathDetailResp detail = learningPathService.detail(first.getId(), null);
        Long questionId = detail.getItems().get(0).getQuestionId();

        userId = userService.create("t26path" + System.currentTimeMillis() % 100000, "123456", 0).getId();
        // 同一题自评两次：进度按去重题数算，仍然只加 1
        insertRecord(questionId, 2);
        insertRecord(questionId, 3);

        PathDetailResp withProgress = learningPathService.detail(first.getId(), userId);
        assertThat(withProgress.getCompletedCount()).isEqualTo(1L);
        assertThat(withProgress.getItems().get(0).getDone()).isTrue();

        PathResp listItem = learningPathService.list(userId).stream()
                .filter(p -> p.getId().equals(first.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(listItem.getCompletedCount()).isEqualTo(1L);
    }

    /** v3.9：路径详情带收藏态（表格「标记」列）。 */
    @Test
    void favoriteFlagReflectedInItems() {
        List<PathResp> paths = learningPathService.list(null);
        assumeThat(paths).as("需要先执行 db/migration-v4-path.sql").isNotEmpty();
        Long pathId = paths.get(0).getId();
        PathDetailResp guestDetail = learningPathService.detail(pathId, null);
        Long favoritedQuestionId = guestDetail.getItems().get(0).getQuestionId();

        userId = userService.create("t29fav" + System.currentTimeMillis() % 100000, "123456", 0).getId();
        jdbcTemplate.update(
                "INSERT IGNORE INTO user_favorite (user_id, question_id, created_at) VALUES (?, ?, NOW(3))",
                userId, favoritedQuestionId);

        PathDetailResp detail = learningPathService.detail(pathId, userId);
        assertThat(detail.getItems().stream()
                .filter(item -> item.getQuestionId().equals(favoritedQuestionId))
                .findFirst()
                .orElseThrow()
                .getFavorite()).isTrue();
        assertThat(detail.getItems().stream()
                .filter(item -> !item.getQuestionId().equals(favoritedQuestionId))
                .allMatch(item -> Boolean.FALSE.equals(item.getFavorite()))).isTrue();

        // 游客没有收藏态：全部 false
        assertThat(guestDetail.getItems()).allSatisfy(item ->
                assertThat(item.getFavorite()).isFalse());
    }

    private void insertRecord(Long questionId, int selfAssess) {
        jdbcTemplate.update(
                "INSERT INTO practice_record (user_id, question_id, self_assess, source, created_at) "
                        + "VALUES (?, ?, ?, 1, NOW(3))",
                userId, questionId, selfAssess);
    }
}
