package com.codeinterview.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Tag(name = "健康检查")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final String VERSION = "1.0.0";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Operation(summary = "健康检查（不包 Result，DB 不通返回 503）")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = checkDb();
        String status = dbUp ? "UP" : "DOWN";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("db", status);
        body.put("version", VERSION);
        body.put("time", LocalDateTime.now().format(TIME_FORMATTER));
        if (dbUp) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean checkDb() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (Exception ex) {
            log.warn("health check db failed: {}", ex.getMessage());
            return false;
        }
    }
}
