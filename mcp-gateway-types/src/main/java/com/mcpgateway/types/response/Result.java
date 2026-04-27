package com.mcpgateway.types.response;

import com.mcpgateway.types.context.ResponseContext;
import com.mcpgateway.types.enums.ResponseCode;

public record Result<T>(String code, String message, String requestId, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResponseCode.SUCCESS.code(),
                ResponseCode.SUCCESS.message(),
                ResponseContext.currentRequestId().orElse(null),
                data
        );
    }

    public static <T> Result<T> failure(ResponseCode responseCode, String message) {
        return new Result<>(
                responseCode.code(),
                message,
                ResponseContext.currentRequestId().orElse(null),
                null
        );
    }

    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(
                code,
                message,
                ResponseContext.currentRequestId().orElse(null),
                null
        );
    }
}
