package com.mcpgateway.types.enums;

public enum ResponseCode {
    SUCCESS("0000", "Success"),
    BAD_REQUEST("A0400", "Bad request"),
    UNAUTHORIZED("A0401", "Unauthorized"),
    FORBIDDEN("A0403", "Forbidden"),
    NOT_FOUND("A0404", "Not found"),
    CONFLICT("A0409", "Conflict"),
    SYSTEM_ERROR("B0500", "System error");

    private final String code;
    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}

