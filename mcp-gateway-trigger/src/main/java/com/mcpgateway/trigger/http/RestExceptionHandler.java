package com.mcpgateway.trigger.http;

import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import com.mcpgateway.types.response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Result<Void>> handleAppException(AppException exception) {
        ErrorResponseSupport.ResultCode resultCode = ErrorResponseSupport.resolveResultCode(exception.getCode());
        log.warn("gateway.business_error code={} message={}", resultCode.code(), exception.getMessage());
        return ResponseEntity.status(ErrorResponseSupport.resolveStatus(resultCode.code()))
                .body(Result.failure(resultCode.code(), resultCode.message()));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception exception) {
        log.warn("gateway.bad_request_error type={} detail={}", exception.getClass().getSimpleName(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failure(ResponseCode.BAD_REQUEST, ResponseCode.BAD_REQUEST.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        log.error("gateway.system_error type={} detail={}", exception.getClass().getName(), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(ResponseCode.SYSTEM_ERROR, ResponseCode.SYSTEM_ERROR.message()));
    }
}
