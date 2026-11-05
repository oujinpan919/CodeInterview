package com.codeinterview.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.codeinterview.common.result.Result;
import com.codeinterview.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
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

    /** 查询参数/路径参数类型不匹配（如 status=abc）按 40000 处理，不能落到 50000（04 A1 参数错误口径）。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(ResultCode.PARAM_ERROR, "参数格式错误: " + ex.getName());
    }

    /** 路径存在但方法不支持（如 GET /admin/tags）：按参数错误而不是 500 返回，避免泄漏栈/状态混淆。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("request method not supported: {} {}", ex.getMethod(), ex.getMessage());
        return build(ResultCode.PARAM_ERROR, "请求方法不支持");
    }

    /** Accept 与接口 produces 不匹配（如 /ai/chat 不接收 text/event-stream）：客户端错误，不能落 500。 */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.warn("media type not acceptable: {}", ex.getMessage());
        return build(ResultCode.PARAM_ERROR, "请求头 Accept 不支持该接口");
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

    /**
     * SSE 异步请求超时（SseEmitter 190s 兜底；正常应由 watchdog 180s 先发 error）。
     * 响应可能已提交，返回 50202 或空响应都不影响客户端已收内容。
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Result<Void>> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        log.warn("[{}] async request timeout", currentTraceId());
        return build(ResultCode.AI_TIMEOUT, null);
    }

    /** 客户端断开后容器再次写响应（05 §9.2「客户端断开/点停止」）：记录即可，不当作业务错误。 */
    @ExceptionHandler({AsyncRequestNotUsableException.class, IOException.class})
    public ResponseEntity<Void> handleClientAborted(Exception ex) {
        log.debug("[{}] response write aborted by client: {}", currentTraceId(), ex.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("[{}] unhandled exception", currentTraceId(), ex);
        return build(ResultCode.SERVER_ERROR, null);
    }

    private ResponseEntity<Result<Void>> build(ResultCode resultCode, String message) {
        // 显式 application/json：/ai/chat 的前端请求带 Accept: text/event-stream，
        // 不指定会产生内容协商失败（406），流前错误必须仍是 Result JSON（05 §4.3）。
        return ResponseEntity.status(resultCode.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.error(resultCode, message));
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
