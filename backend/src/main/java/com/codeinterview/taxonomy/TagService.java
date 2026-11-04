package com.codeinterview.taxonomy;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codeinterview.taxonomy.vo.TagResp;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public List<TagResp> list() {
        return tagMapper.selectWithPublishedCount();
    }
}
