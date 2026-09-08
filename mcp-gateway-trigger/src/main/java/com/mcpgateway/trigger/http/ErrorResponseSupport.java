package com.mcpgateway.trigger.http;

import com.mcpgateway.types.enums.ResponseCode;
import org.springframework.http.HttpStatus;

public final class ErrorResponseSupport {

    private ErrorResponseSupport() {
    }

    public static HttpStatus resolveStatus(String code) {
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

    public static ResultCode resolveResultCode(String code) {
        ResponseCode responseCode = ResponseCode.fromCode(code);
        return new ResultCode(responseCode.code(), responseCode.message());
    }

    public record ResultCode(String code, String message) {
    }
}
