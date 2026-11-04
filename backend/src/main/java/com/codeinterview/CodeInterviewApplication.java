package com.codeinterview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.codeinterview")
public class CodeInterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeInterviewApplication.class, args);
    }
}
