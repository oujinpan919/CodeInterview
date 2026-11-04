package com.codeinterview.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.codeinterview.common.result.Result;
import com.codeinterview.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        log.warn("business exception code={} message={}", ex.getResultCode().getCode(), ex.getMessage());
        return build(ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describeFieldError)
                .collect(Collectors.joining("; "));
        return build(ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(this::describeConstraintViolation)
                .collect(Collectors.joining("; "));
        return build(ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("request body not readable: {}", ex.getMessage());
        return build(ResultCode.PARAM_ERROR, "请求体格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(ResultCode.PARAM_ERROR, "缺少参数: " + ex.getParameterName());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Result<Void>> handleNoHandler(Exception ex) {
        log.warn("no handler found: {}", ex.getMessage());
        return build(ResultCode.NOT_FOUND, null);
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException ex) {
        log.warn("not login: {}", ex.getMessage());
        return build(ResultCode.UNAUTHORIZED, null);
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<Result<Void>> handleNotRole(NotRoleException ex) {
        log.warn("not role: {}", ex.getMessage());
        return build(ResultCode.ADMIN_REQUIRED, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("[{}] unhandled exception", currentTraceId(), ex);
        return build(ResultCode.SERVER_ERROR, null);
    }

    private ResponseEntity<Result<Void>> build(ResultCode resultCode, String message) {
        return ResponseEntity.status(resultCode.getHttpStatus()).body(Result.error(resultCode, message));
    }

    private String describeFieldError(FieldError fieldError) {
        String field = fieldError.getField();
        String defaultMessage = fieldError.getDefaultMessage();
        return (defaultMessage == null || defaultMessage.isBlank()) ? field : field + ": " + defaultMessage;
    }

    private String describeConstraintViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId == null ? "-" : traceId;
    }
}
