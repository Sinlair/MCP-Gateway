package com.mcpgateway.types.exception;

import com.mcpgateway.types.enums.ResponseCode;

public class AppException extends RuntimeException {

    private final String code;

    public AppException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.code();
    }

    public AppException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

