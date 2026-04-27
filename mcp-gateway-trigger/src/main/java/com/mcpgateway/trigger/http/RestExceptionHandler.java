package com.mcpgateway.trigger.http;

import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import com.mcpgateway.types.response.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Result<Void>> handleAppException(AppException exception) {
        return ResponseEntity.status(resolveStatus(exception.getCode()))
                .body(Result.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(ResponseCode.SYSTEM_ERROR, exception.getMessage()));
    }

    private HttpStatus resolveStatus(String code) {
        if (ResponseCode.BAD_REQUEST.code().equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ResponseCode.UNAUTHORIZED.code().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ResponseCode.FORBIDDEN.code().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (ResponseCode.NOT_FOUND.code().equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (ResponseCode.CONFLICT.code().equals(code)) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

