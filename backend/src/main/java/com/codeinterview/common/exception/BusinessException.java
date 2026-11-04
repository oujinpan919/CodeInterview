package com.codeinterview.common.exception;

import com.codeinterview.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String customMessage) {
        super((customMessage == null || customMessage.isBlank()) ? resultCode.getMessage() : customMessage);
        this.resultCode = resultCode;
    }
}
