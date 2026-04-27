package com.mcpgateway.types.context;

import java.util.Optional;

public final class ResponseContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private ResponseContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static Optional<String> currentRequestId() {
        return Optional.ofNullable(REQUEST_ID.get());
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
