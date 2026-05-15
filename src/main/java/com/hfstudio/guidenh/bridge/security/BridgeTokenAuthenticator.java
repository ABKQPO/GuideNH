package com.hfstudio.guidenh.bridge.security;

public class BridgeTokenAuthenticator {

    private final String token;

    public BridgeTokenAuthenticator(String token) {
        this.token = token == null ? "" : token;
    }

    public boolean matches(String candidate) {
        if (token.isEmpty() || candidate == null) {
            return false;
        }
        return constantTimeEquals(token, candidate);
    }

    public String redact(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return "[redacted]";
    }

    private boolean constantTimeEquals(String left, String right) {
        int result = left.length() ^ right.length();
        int max = Math.max(left.length(), right.length());
        for (int index = 0; index < max; index++) {
            char leftChar = index < left.length() ? left.charAt(index) : 0;
            char rightChar = index < right.length() ? right.charAt(index) : 0;
            result |= leftChar ^ rightChar;
        }
        return result == 0;
    }
}
