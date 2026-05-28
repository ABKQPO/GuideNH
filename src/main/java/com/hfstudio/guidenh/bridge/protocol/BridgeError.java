package com.hfstudio.guidenh.bridge.protocol;

public class BridgeError {

    private final String code;
    private final String message;
    private final boolean retryable;

    public BridgeError(String code, String message, boolean retryable) {
        this.code = code;
        this.message = message;
        this.retryable = retryable;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
