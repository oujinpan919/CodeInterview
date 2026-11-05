package com.codeinterview.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 纯单测（无 Spring 上下文）：05 §12 #13 的 usage 缺失估算，以及 §2.2/§2.3/§9.3 的解析口径。
 * 覆盖：分段拼接、finish_reason、code != 0、usage 缺失即 estimated=1、
 * reasoning_content 忽略、security_suggest=HIDE_CONTINUE 跳过该段。
 */
class UpstreamSseParserTest {

    private final UpstreamSseParser parser = new UpstreamSseParser(new ObjectMapper());

    @Test
    void deltaContentIsParsedAndMultiLinesAreConcatenated() {
        String first = "{\"code\":0,\"message\":\"Success\",\"sid\":\"s1\",\"choices\":[{\"delta\":{\"content\":\"HashMap \"},\"index\":0}]}";
        String second = "{\"code\":0,\"message\":\"Success\",\"sid\":\"s1\",\"choices\":[{\"delta\":{\"content\":\"扩容\"},\"index\":0}]}";
        String third = "{\"code\":0,\"message\":\"Success\",\"sid\":\"s1\",\"choices\":[{\"delta\":{\"content\":\"机制\"},\"index\":0}]}";

        StringBuilder joined = new StringBuilder();
        for (String line : List.of(first, second, third)) {
            UpstreamSseParser.ParseResult result = parser.parse(line);
            assertThat(result.code()).isZero();
            assertThat(result.hasDelta()).isTrue();
            joined.append(result.delta());
        }
        assertThat(joined.toString()).isEqualTo("HashMap 扩容机制");
    }

    @Test
    void finishReasonAndUsageAreParsed() {
        String line = "{\"code\":0,\"message\":\"Success\",\"sid\":\"s1\","
                + "\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\",\"index\":0}],"
                + "\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":20,\"total_tokens\":32}}";
        UpstreamSseParser.ParseResult result = parser.parse(line);

        assertThat(result.finishReason()).isEqualTo("stop");
        assertThat(result.hasDelta()).isFalse();
        assertThat(result.hasUsage()).isTrue();
        assertThat(result.usage().promptTokens()).isEqualTo(12);
        assertThat(result.usage().completionTokens()).isEqualTo(20);
        assertThat(result.usage().totalTokens()).isEqualTo(32);
        assertThat(result.usage().estimated()).isZero();
    }

    @Test
    void searchPromptTokensAreAddedIntoPromptTokens() {
        String line = "{\"code\":0,\"choices\":[{\"delta\":{}}],"
                + "\"usage\":{\"prompt_tokens\":10,\"search_prompt_tokens\":5,"
                + "\"completion_tokens\":7,\"total_tokens\":22}}";
        UpstreamSseParser.Usage usage = parser.parse(line).usage();

        assertThat(usage.promptTokens()).isEqualTo(15);
        assertThat(usage.totalTokens()).isEqualTo(22);
    }

    @Test
    void missingUsageIsEstimatedWithEstimatedFlag() {
        // ceil(16 / 1.6) = 10，ceil(32 / 1.6) = 20
        UpstreamSseParser.Usage usage = parser.estimateUsage(16, 32);

        assertThat(usage.promptTokens()).isEqualTo(10);
        assertThat(usage.completionTokens()).isEqualTo(20);
        assertThat(usage.totalTokens()).isEqualTo(30);
        assertThat(usage.estimated()).isEqualTo(1);
    }

    @Test
    void codeNotZeroIsErrorAndContentIsNotAccumulated() {
        String line = "{\"code\":11201,\"message\":\"day quota exceeded\",\"sid\":\"s9\","
                + "\"choices\":[{\"delta\":{\"content\":\"不应被当成正常内容\"},\"index\":0}]}";
        UpstreamSseParser.ParseResult result = parser.parse(line);

        assertThat(result.isError()).isTrue();
        assertThat(result.code()).isEqualTo(11201);
        assertThat(result.message()).isEqualTo("day quota exceeded");
        assertThat(result.hasDelta()).isFalse();
    }

    @Test
    void reasoningContentIsIgnored() {
        String line = "{\"code\":0,\"choices\":[{\"delta\":{\"reasoning_content\":\"思考过程\",\"content\":\"\"},\"index\":0}]}";
        UpstreamSseParser.ParseResult result = parser.parse(line);

        assertThat(result.code()).isZero();
        assertThat(result.hasDelta()).isFalse();
    }

    @Test
    void securitySuggestHideContinueSkipsThatSegment() {
        String line = "{\"code\":0,\"choices\":[{\"delta\":{\"content\":\"敏感片段\","
                + "\"security_suggest\":{\"action\":\"HIDE_CONTINUE\"}},\"index\":0}]}";
        UpstreamSseParser.ParseResult result = parser.parse(line);

        assertThat(result.securityHide()).isTrue();
        assertThat(result.hasDelta()).isFalse();
    }

    @Test
    void malformedLineIsSkippedWithoutThrowing() {
        UpstreamSseParser.ParseResult result = parser.parse("not-a-json");
        assertThat(result.code()).isZero();
        assertThat(result.hasDelta()).isFalse();
    }
}
