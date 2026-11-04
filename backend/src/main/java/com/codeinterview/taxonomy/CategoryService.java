package com.codeinterview.taxonomy;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codeinterview.taxonomy.vo.CategoryResp;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<CategoryResp> listEnabled() {
        return categoryMapper.selectEnabledWithQuestionCount();
    }
}
