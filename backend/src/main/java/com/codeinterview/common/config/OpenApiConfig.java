package com.codeinterview.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI codeInterviewOpenApi() {
        return new OpenAPI().info(new Info()
                .title("码上记 CodeInterview API")
                .description("面试题库 · 三档自评 · 错题复习")
                .version("1.0.0"));
    }
}
