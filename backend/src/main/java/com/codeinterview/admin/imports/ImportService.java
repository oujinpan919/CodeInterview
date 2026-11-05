package com.codeinterview.admin.imports;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.codeinterview.admin.questions.AdminQuestionMapper;
import com.codeinterview.admin.taxonomy.AdminTaxonomyMapper;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.question.entity.Question;
import com.codeinterview.taxonomy.entity.Category;
import com.codeinterview.taxonomy.entity.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 批量导入（01 F09 / 03 §7 / 04 A3.9 #34）。
 * 规则：整批校验（≤1MB、1–50 题、JSON 语法）不通过则整批 40000；
 * 逐条处理，单条失败只记该条 failed，不影响其他条；重复（同分类 title+content_md）记 skipped。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private static final int MAX_QUESTIONS = 50;
    private static final int MAX_TAGS = 10;
    private static final int MAX_JSON_CHARS = 1_000_000;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_CONTENT_LENGTH = 20000;
    private static final int MAX_SOURCE_LENGTH = 50;
    private static final int MAX_TAG_NAME_LENGTH = 20;
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final ObjectMapper objectMapper;
    private final AdminQuestionMapper adminQuestionMapper;
    private final AdminTaxonomyMapper adminTaxonomyMapper;
    private final ImportMapper importMapper;

    public ImportResult importJson(Long userId, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "json 不能为空");
        }
        if (rawJson.length() > MAX_JSON_CHARS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "单次导入内容不能超过 1MB");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "JSON 语法错误：" + ex.getOriginalMessage());
        }
        if (root == null || !root.isObject()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "导入内容必须是 JSON 对象");
        }
        JsonNode questionsNode = root.get("questions");
        if (questionsNode == null || !questionsNode.isArray()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "questions 必须是数组");
        }
        if (questionsNode.isEmpty() || questionsNode.size() > MAX_QUESTIONS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "questions 数量必须为 1–50");
        }
        CategoryRef defaultCategory = parseTopCategory(root.get("category"));
        List<String> defaultTags = parseTopTags(root.get("tags"));

        ImportResult result = new ImportResult();
        Map<String, Long> categoryCache = new HashMap<>();
        Map<String, Long> tagCache = new HashMap<>();
        int index = 0;
        for (JsonNode node : questionsNode) {
            try {
                importOne(node, userId, defaultCategory, defaultTags, categoryCache, tagCache, result);
            } catch (ItemException ex) {
                result.addError(index, ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("import item failed, index={}, cause={}", index, ex.getMessage());
                result.addError(index, "入库失败：" + ex.getMessage());
            }
            index++;
        }
        return result;
    }

    private void importOne(JsonNode node, Long userId, CategoryRef defaultCategory,
                           List<String> defaultTags, Map<String, Long> categoryCache,
                           Map<String, Long> tagCache, ImportResult result) {
        if (node == null || !node.isObject()) {
            throw new ItemException("题目必须是对象");
        }
        String title = requireText(node, "title", "title 不能为空", MAX_TITLE_LENGTH).trim();
        String contentMd = requireText(node, "content_md", "content_md 不能为空", MAX_CONTENT_LENGTH);
        String answerMd = requireText(node, "answer_md", "answer_md 不能为空", MAX_CONTENT_LENGTH);
        int difficulty = requireInt(node, "difficulty");
        if (difficulty < 1 || difficulty > 3) {
            throw new ItemException("difficulty 只能为 1/2/3");
        }
        String source = optionalText(node, "source", MAX_SOURCE_LENGTH, "");
        int status = optionalInt(node, "status", STATUS_DRAFT);
        if (status != STATUS_DRAFT && status != STATUS_PUBLISHED) {
            throw new ItemException("status 只能为 0 或 1");
        }

        CategoryRef categoryRef = node.hasNonNull("category")
                ? parseItemCategory(node.get("category"))
                : defaultCategory;
        if (categoryRef == null || categoryRef.name() == null || categoryRef.name().isBlank()) {
            throw new ItemException("缺少分类（顶层 category 或单题 category）");
        }
        Long categoryId = findOrCreateCategory(categoryRef, categoryCache);

        List<String> mergedTags = mergeTags(defaultTags, parseItemTags(node.get("tags")));
        if (mergedTags.size() > MAX_TAGS) {
            throw new ItemException("最多 10 个标签");
        }
        List<Long> tagIds = new ArrayList<>(mergedTags.size());
        for (String tagName : mergedTags) {
            tagIds.add(findOrCreateTag(tagName, tagCache));
        }

        if (importMapper.selectDuplicateId(categoryId, title, contentMd) != null) {
            result.markSkipped();
            return;
        }

        Question question = new Question();
        question.setTitle(title);
        question.setContentMd(contentMd);
        question.setAnswerMd(answerMd);
        question.setCategoryId(categoryId);
        question.setDifficulty(difficulty);
        question.setStatus(status);
        question.setSource(source);
        question.setCreatedBy(userId);
        if (status == STATUS_PUBLISHED) {
            question.setPublishedAt(LocalDateTime.now(SHANGHAI));
        }
        adminQuestionMapper.insert(question);
        if (!tagIds.isEmpty()) {
            adminQuestionMapper.insertTags(question.getId(), tagIds);
        }
        result.markCreated();
    }

    private Long findOrCreateCategory(CategoryRef ref, Map<String, Long> cache) {
        String name = ref.name().trim();
        Long cached = cache.get(name);
        if (cached != null) {
            return cached;
        }
        Long id = adminTaxonomyMapper.selectCategoryIdByName(name);
        if (id == null) {
            Category category = new Category();
            category.setName(name);
            category.setDescription(ref.description() == null ? "" : ref.description());
            category.setSort(ref.sort() == null ? 0 : ref.sort());
            category.setStatus(Category.STATUS_ENABLED);
            try {
                adminTaxonomyMapper.insertCategory(category);
                id = category.getId();
            } catch (DuplicateKeyException ex) {
                id = adminTaxonomyMapper.selectCategoryIdByName(name);
            }
        }
        if (id == null) {
            throw new ItemException("分类创建失败：" + name);
        }
        cache.put(name, id);
        return id;
    }

    private Long findOrCreateTag(String name, Map<String, Long> cache) {
        String trimmed = name.trim();
        Long cached = cache.get(trimmed);
        if (cached != null) {
            return cached;
        }
        Long id = adminTaxonomyMapper.selectTagIdByName(trimmed);
        if (id == null) {
            Tag tag = new Tag();
            tag.setName(trimmed);
            try {
                adminTaxonomyMapper.insertTag(tag);
                id = tag.getId();
            } catch (DuplicateKeyException ex) {
                id = adminTaxonomyMapper.selectTagIdByName(trimmed);
            }
        }
        if (id == null) {
            throw new ItemException("标签创建失败：" + trimmed);
        }
        cache.put(trimmed, id);
        return id;
    }

    /** 顶层 category 支持字符串或对象；非法类型属于整批参数错误。 */
    private CategoryRef parseTopCategory(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return parseCategory(node);
        } catch (ItemException ex) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "顶层 category 格式错误：" + ex.getMessage());
        }
    }

    private CategoryRef parseItemCategory(JsonNode node) {
        return parseCategory(node);
    }

    private CategoryRef parseCategory(JsonNode node) {
        if (node.isTextual()) {
            String name = node.asText().trim();
            if (name.isEmpty()) {
                throw new ItemException("category 不能为空");
            }
            return new CategoryRef(name, null, null);
        }
        if (!node.isObject()) {
            throw new ItemException("category 必须是字符串或对象");
        }
        String name = node.hasNonNull("name") ? node.get("name").asText().trim() : "";
        if (name.isEmpty()) {
            throw new ItemException("category.name 不能为空");
        }
        String description = node.hasNonNull("description") ? node.get("description").asText() : null;
        Integer sort = node.hasNonNull("sort") && node.get("sort").canConvertToInt()
                ? node.get("sort").asInt() : null;
        return new CategoryRef(name, description, sort);
    }

    private List<String> parseTopTags(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        try {
            return parseTags(node);
        } catch (ItemException ex) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "顶层 tags 格式错误：" + ex.getMessage());
        }
    }

    private List<String> parseItemTags(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        return parseTags(node);
    }

    private List<String> parseTags(JsonNode node) {
        if (!node.isArray()) {
            throw new ItemException("tags 必须是数组");
        }
        List<String> tags = new ArrayList<>(node.size());
        for (JsonNode tag : node) {
            if (!tag.isTextual()) {
                throw new ItemException("tags 只能包含字符串");
            }
            String name = tag.asText().trim();
            if (name.isEmpty() || name.length() > MAX_TAG_NAME_LENGTH) {
                throw new ItemException("标签名必须为 1–20 字");
            }
            tags.add(name);
        }
        return tags;
    }

    private List<String> mergeTags(List<String> first, List<String> second) {
        Set<String> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return new ArrayList<>(merged);
    }

    private String requireText(JsonNode node, String field, String message, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ItemException(message);
        }
        String text = value.asText();
        if (text.length() > maxLength) {
            throw new ItemException(field + " 不能超过 " + maxLength + " 字符");
        }
        return text;
    }

    private int requireInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToInt()) {
            throw new ItemException(field + " 不能为空");
        }
        return value.asInt();
    }

    private String optionalText(JsonNode node, String field, int maxLength, String defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String text = value.asText();
        if (text.length() > maxLength) {
            throw new ItemException(field + " 不能超过 " + maxLength + " 字符");
        }
        return text;
    }

    private int optionalInt(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (!value.canConvertToInt()) {
            throw new ItemException(field + " 格式错误");
        }
        return value.asInt();
    }

    /** 单条导入失败：只影响该条。 */
    private static class ItemException extends RuntimeException {
        ItemException(String message) {
            super(message);
        }
    }

    private record CategoryRef(String name, String description, Integer sort) {
    }
}
