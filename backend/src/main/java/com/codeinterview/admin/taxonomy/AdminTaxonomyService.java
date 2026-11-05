package com.codeinterview.admin.taxonomy;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.codeinterview.admin.dto.CategorySaveReq;
import com.codeinterview.admin.dto.TagSaveReq;
import com.codeinterview.admin.vo.AdminCategoryResp;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.taxonomy.entity.Category;
import com.codeinterview.taxonomy.entity.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 管理端分类/标签维护（01 F09 / 04 A3.9 #36–#41）。
 * 名称唯一靠库唯一键判定（捕获 DuplicateKeyException → 40900），引用校验 → 40901。
 * 写操作按 02 §5.6 打审计日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTaxonomyService {

    private static final int DEFAULT_SORT = 0;
    private static final int DEFAULT_STATUS = Category.STATUS_ENABLED;

    private final AdminTaxonomyMapper adminTaxonomyMapper;

    public List<AdminCategoryResp> categories() {
        return adminTaxonomyMapper.selectAdminCategories();
    }

    public Long createCategory(CategorySaveReq req) {
        Category category = new Category();
        category.setName(req.getName().trim());
        category.setDescription(normalizeDescription(req.getDescription()));
        category.setSort(req.getSort() == null ? DEFAULT_SORT : req.getSort());
        category.setStatus(req.getStatus() == null ? DEFAULT_STATUS : req.getStatus());
        try {
            adminTaxonomyMapper.insertCategory(category);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.CONFLICT, "分类名已存在");
        }
        audit("CATEGORY_CREATE", category.getId());
        return category.getId();
    }

    public void updateCategory(Long id, CategorySaveReq req) {
        if (adminTaxonomyMapper.countCategoryById(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Category category = new Category();
        category.setId(id);
        category.setName(req.getName().trim());
        category.setDescription(normalizeDescription(req.getDescription()));
        category.setSort(req.getSort() == null ? DEFAULT_SORT : req.getSort());
        category.setStatus(req.getStatus() == null ? DEFAULT_STATUS : req.getStatus());
        try {
            adminTaxonomyMapper.updateCategory(category);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.CONFLICT, "分类名已存在");
        }
        audit("CATEGORY_UPDATE", id);
    }

    public void deleteCategory(Long id) {
        if (adminTaxonomyMapper.countCategoryById(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (adminTaxonomyMapper.countQuestionsByCategory(id) > 0) {
            throw new BusinessException(ResultCode.REFERENCE_CONFLICT, "该分类下还有题目，不能删除；可改为停用");
        }
        adminTaxonomyMapper.deleteCategory(id);
        audit("CATEGORY_DELETE", id);
    }

    public Long createTag(TagSaveReq req) {
        Tag tag = new Tag();
        tag.setName(req.getName().trim());
        try {
            adminTaxonomyMapper.insertTag(tag);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.CONFLICT, "标签名已存在");
        }
        audit("TAG_CREATE", tag.getId());
        return tag.getId();
    }

    public void updateTag(Long id, TagSaveReq req) {
        if (adminTaxonomyMapper.countTagById(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(req.getName().trim());
        try {
            adminTaxonomyMapper.updateTag(tag);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.CONFLICT, "标签名已存在");
        }
        audit("TAG_UPDATE", id);
    }

    public void deleteTag(Long id) {
        if (adminTaxonomyMapper.countTagById(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (adminTaxonomyMapper.countQuestionTagRefs(id) > 0) {
            throw new BusinessException(ResultCode.REFERENCE_CONFLICT, "标签已被题目引用，需先解除关联");
        }
        adminTaxonomyMapper.deleteTag(id);
        audit("TAG_DELETE", id);
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    /** 02 §5.6：管理端写操作审计（V1 只落应用日志）。 */
    private void audit(String action, Long targetId) {
        log.info("audit action={} userId={} targetId={} result=OK", action,
                StpUtil.getLoginIdAsLong(), targetId);
    }
}
