package com.codeinterview.admin.questions;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeinterview.admin.dto.QuestionSaveReq;
import com.codeinterview.admin.vo.AdminQuestionDetailResp;
import com.codeinterview.admin.vo.AdminQuestionListItemResp;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.common.util.PageUtil;
import com.codeinterview.question.entity.Question;

import lombok.RequiredArgsConstructor;

/**
 * 管理端题目维护（01 F09 / 04 A3.9 #27–#33）。
 * 状态流转见 03 §4.1：新建=草稿、发布写 published_at（仅首次）、下架=2、逻辑删除 deleted=1。
 */
@Service
@RequiredArgsConstructor
public class AdminQuestionService {

    private static final int MAX_KEYWORD_LENGTH = 30;
    private static final int MAX_TAGS = 10;
    private static final int MAX_SOURCE_LENGTH = 50;
    private static final int CATEGORY_ENABLED = 1;
    private static final int MAX_TITLE_LENGTH = 120;

    private final AdminQuestionMapper adminQuestionMapper;

    public PageResp<AdminQuestionListItemResp> page(Integer rawStatus, Long categoryId, String rawKeyword,
                                                    Integer rawPage, Integer rawPageSize) {
        int page = PageUtil.normalizePage(rawPage);
        int pageSize = PageUtil.normalizePageSize(rawPageSize);
        Integer status = (rawStatus != null && rawStatus >= Question.STATUS_DRAFT
                && rawStatus <= Question.STATUS_OFFLINE) ? rawStatus : null;
        Long normalizedCategory = categoryId != null && categoryId > 0 ? categoryId : null;
        String keyword = normalizeKeyword(rawKeyword);
        long total = adminQuestionMapper.countAdminPage(status, normalizedCategory, keyword);
        if (total == 0) {
            return PageResp.of(page, pageSize, 0, List.of());
        }
        long offset = PageUtil.offset(page, pageSize);
        List<AdminQuestionListItemResp> list =
                adminQuestionMapper.selectAdminPage(status, normalizedCategory, keyword, offset, pageSize);
        return PageResp.of(page, pageSize, total, list);
    }

    public AdminQuestionDetailResp detail(Long id) {
        AdminQuestionDetailResp detail = adminQuestionMapper.selectAdminDetail(id);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        detail.setTagIds(adminQuestionMapper.selectTagIds(id));
        return detail;
    }

    @Transactional
    public Long create(QuestionSaveReq req, Long userId) {
        requireCategory(req.getCategoryId(), null);
        List<Long> tagIds = normalizeTagIds(req.getTagIds());
        Question question = new Question();
        question.setTitle(req.getTitle().trim());
        question.setContentMd(req.getContentMd());
        question.setAnswerMd(req.getAnswerMd());
        question.setCategoryId(req.getCategoryId());
        question.setDifficulty(req.getDifficulty());
        question.setSource(normalizeSource(req.getSource()));
        question.setStatus(Question.STATUS_DRAFT);
        question.setCreatedBy(userId);
        adminQuestionMapper.insert(question);
        replaceTags(question.getId(), tagIds);
        return question.getId();
    }

    @Transactional
    public void update(Long id, QuestionSaveReq req) {
        Question existing = adminQuestionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        requireCategory(req.getCategoryId(), existing.getCategoryId());
        List<Long> tagIds = normalizeTagIds(req.getTagIds());
        Question patch = new Question();
        patch.setId(id);
        patch.setTitle(req.getTitle().trim());
        patch.setContentMd(req.getContentMd());
        patch.setAnswerMd(req.getAnswerMd());
        patch.setCategoryId(req.getCategoryId());
        patch.setDifficulty(req.getDifficulty());
        patch.setSource(normalizeSource(req.getSource()));
        if (adminQuestionMapper.updateById(patch) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        replaceTags(id, tagIds);
    }

    /** 逻辑删除：不做引用检查（01 F09）；已收藏/错题行保留但前台按 deleted=0 过滤。 */
    public void delete(Long id) {
        if (adminQuestionMapper.deleteById(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    /** 发布前校验必填项齐全（04 A3.9 #32），缺哪项就指出哪个字段。 */
    @Transactional
    public int publish(Long id) {
        AdminQuestionDetailResp detail = detail(id);
        StringBuilder missing = new StringBuilder();
        if (isBlank(detail.getTitle())) {
            missing.append("标题不能为空;");
        }
        if (isBlank(detail.getContentMd())) {
            missing.append("题干不能为空;");
        }
        if (isBlank(detail.getAnswerMd())) {
            missing.append("参考答案不能为空;");
        }
        if (detail.getCategoryId() == null
                || adminQuestionMapper.selectCategoryStatus(detail.getCategoryId()) == null) {
            missing.append("分类不能为空;");
        }
        if (detail.getDifficulty() == null) {
            missing.append("难度不能为空;");
        }
        if (missing.length() > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, missing.toString());
        }
        if (adminQuestionMapper.publish(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return Question.STATUS_PUBLISHED;
    }

    public int offline(Long id) {
        if (adminQuestionMapper.offline(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return Question.STATUS_OFFLINE;
    }

    /** 分类必须存在且启用；编辑已停用分类的旧题时允许保留原分类（04 A3.9 #29）。 */
    private void requireCategory(Long categoryId, Long existingCategoryId) {
        Integer status = adminQuestionMapper.selectCategoryStatus(categoryId);
        if (status == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类不存在");
        }
        if (status != CATEGORY_ENABLED && !Objects.equals(categoryId, existingCategoryId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类已停用，请选择启用的分类");
        }
    }

    /** null = 不改标签（仅更新场景）；空数组 = 清空；否则覆盖式重建（04 A3.9 #29）。 */
    private void replaceTags(Long questionId, List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        adminQuestionMapper.deleteTags(questionId);
        if (!tagIds.isEmpty()) {
            adminQuestionMapper.insertTags(questionId, tagIds);
        }
    }

    private List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            return null;
        }
        List<Long> ids = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.size() > MAX_TAGS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "最多选择 10 个标签");
        }
        if (!ids.isEmpty() && adminQuestionMapper.countExistingTags(ids) != ids.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "标签不存在");
        }
        return ids;
    }

    private String normalizeSource(String source) {
        if (source == null) {
            return "";
        }
        String trimmed = source.trim();
        return trimmed.length() > MAX_SOURCE_LENGTH ? trimmed.substring(0, MAX_SOURCE_LENGTH) : trimmed;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            trimmed = trimmed.substring(0, MAX_KEYWORD_LENGTH);
        }
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            trimmed = trimmed.substring(0, MAX_TITLE_LENGTH);
        }
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
