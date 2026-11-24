package com.codeinterview.ai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeinterview.ai.dto.PromptTemplateSaveReq;
import com.codeinterview.ai.entity.AiPromptTemplate;
import com.codeinterview.ai.mapper.AiPromptTemplateMapper;
import com.codeinterview.ai.vo.PromptTemplateResp;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;

import lombok.RequiredArgsConstructor;

/**
 * Prompt 模板（D11）。用户只能传 {@code templateId}，正文由服务端查库——
 * 这样既满足「可切换角色」又不打开提示词注入面（08 §7 S5）。
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final AiPromptTemplateMapper templateMapper;

    /** 对话页下拉用：只返回启用项，且**不含正文**。 */
    public List<PromptTemplateResp> listEnabled() {
        List<AiPromptTemplate> rows = templateMapper.selectEnabled();
        List<PromptTemplateResp> result = new ArrayList<>(rows.size());
        for (AiPromptTemplate row : rows) {
            result.add(toResp(row, false));
        }
        return result;
    }

    /** 管理端用：含正文。 */
    public List<PromptTemplateResp> listAll() {
        List<AiPromptTemplate> rows = templateMapper.selectAll();
        List<PromptTemplateResp> result = new ArrayList<>(rows.size());
        for (AiPromptTemplate row : rows) {
            result.add(toResp(row, true));
        }
        return result;
    }

    /** 按 id 取正文；不存在或已停用返回 null（调用方回退默认模板）。 */
    public String content(Long templateId) {
        if (templateId == null) {
            return null;
        }
        AiPromptTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getEnabled() == null || template.getEnabled() != 1) {
            return null;
        }
        return template.getContent();
    }

    @Transactional
    public PromptTemplateResp create(PromptTemplateSaveReq req) {
        ensureNameFree(req.getName(), null);
        AiPromptTemplate template = new AiPromptTemplate();
        template.setName(req.getName().trim());
        template.setContent(req.getContent());
        template.setDescription(req.getDescription() == null ? "" : req.getDescription());
        template.setIsBuiltin(0);
        template.setEnabled(req.getEnabled() == null || req.getEnabled() ? 1 : 0);
        template.setSort(req.getSort() == null ? 100 : req.getSort());
        templateMapper.insert(template);
        return toResp(template, true);
    }

    @Transactional
    public PromptTemplateResp update(Long id, PromptTemplateSaveReq req) {
        AiPromptTemplate template = requireExisting(id);
        ensureNameFree(req.getName(), id);
        template.setName(req.getName().trim());
        template.setContent(req.getContent());
        template.setDescription(req.getDescription() == null ? "" : req.getDescription());
        if (req.getEnabled() != null) {
            template.setEnabled(req.getEnabled() ? 1 : 0);
        }
        if (req.getSort() != null) {
            template.setSort(req.getSort());
        }
        templateMapper.updateById(template);
        return toResp(template, true);
    }

    @Transactional
    public void delete(Long id) {
        AiPromptTemplate template = requireExisting(id);
        if (template.getIsBuiltin() != null && template.getIsBuiltin() == 1) {
            throw new BusinessException(ResultCode.REFERENCE_CONFLICT, "内置模板不可删除，可停用");
        }
        templateMapper.deleteById(id);
    }

    private AiPromptTemplate requireExisting(Long id) {
        AiPromptTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return template;
    }

    private void ensureNameFree(String name, Long excludeId) {
        String target = name.trim();
        for (AiPromptTemplate template : templateMapper.selectAll()) {
            if (excludeId != null && excludeId.equals(template.getId())) {
                continue;
            }
            if (target.equals(template.getName())) {
                throw new BusinessException(ResultCode.CONFLICT, "该模板名已存在");
            }
        }
    }

    private PromptTemplateResp toResp(AiPromptTemplate template, boolean withContent) {
        PromptTemplateResp resp = new PromptTemplateResp();
        resp.setId(template.getId());
        resp.setName(template.getName());
        resp.setDescription(template.getDescription());
        resp.setIsBuiltin(template.getIsBuiltin() != null && template.getIsBuiltin() == 1);
        resp.setEnabled(template.getEnabled() != null && template.getEnabled() == 1);
        resp.setSort(template.getSort());
        if (withContent) {
            resp.setContent(template.getContent());
        }
        return resp;
    }
}
