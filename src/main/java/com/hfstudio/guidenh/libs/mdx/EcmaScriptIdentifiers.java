package com.hfstudio.guidenh.libs.mdx;

/**
 * EcmaScript identifier character checks.
 *
 */
public final class EcmaScriptIdentifiers {

    private EcmaScriptIdentifiers() {}

    public static boolean isStart(int code) {
        return Character.isJavaIdentifierStart(code);
    }

    public static boolean isCont(int code) {
        return Character.isJavaIdentifierPart(code);
    }
}
