package com.hfstudio.guidenh.libs.mdx;

public final class MdxCommentMasker {

    private MdxCommentMasker() {}

    public static String mask(String markdown) {
        char[] chars = markdown.toCharArray();

        boolean inFence = false;
        char fenceChar = 0;
        int fenceLength = 0;
        int inlineCodeFence = 0;
        boolean inComment = false;

        int lineStart = 0;
        while (lineStart <= chars.length) {
            int lineEnd = lineStart;
            while (lineEnd < chars.length && chars[lineEnd] != '\n') {
                lineEnd++;
            }

            if (!inComment) {
                Fence fence = detectFence(chars, lineStart, lineEnd);
                if (fence != null) {
                    if (!inFence) {
                        inFence = true;
                        fenceChar = fence.fenceChar;
                        fenceLength = fence.fenceLength;
                    } else if (fence.fenceChar == fenceChar && fence.fenceLength >= fenceLength && fence.closingFence) {
                        inFence = false;
                    }
                }
            }

            if (!inFence) {
                int i = lineStart;
                while (i < lineEnd) {
                    if (inComment) {
                        int closeEnd = findCommentClose(chars, i, lineEnd);
                        if (closeEnd != -1) {
                            maskRange(chars, i, closeEnd + 1);
                            inComment = false;
                            i = closeEnd + 1;
                            continue;
                        }
                        maskRange(chars, i, lineEnd);
                        i = lineEnd;
                        continue;
                    }

                    if (inlineCodeFence != 0) {
                        int runLength = countRun(chars, i, lineEnd, '`');
                        if (runLength == inlineCodeFence) {
                            inlineCodeFence = 0;
                            i += runLength;
                            continue;
                        }
                        i++;
                        continue;
                    }

                    int runLength = countRun(chars, i, lineEnd, '`');
                    if (runLength > 0) {
                        inlineCodeFence = runLength;
                        i += runLength;
                        continue;
                    }

                    if (startsComment(chars, i, lineEnd)) {
                        inComment = true;
                        continue;
                    }

                    i++;
                }
            }

            if (lineEnd == chars.length) {
                break;
            }
            lineStart = lineEnd + 1;
        }

        return new String(chars);
    }

    private static boolean startsComment(char[] chars, int index, int lineEnd) {
        return index + 2 < lineEnd && chars[index] == '{' && chars[index + 1] == '/' && chars[index + 2] == '*';
    }

    private static int findCommentClose(char[] chars, int index, int lineEnd) {
        for (int i = index; i + 2 < lineEnd; i++) {
            if (chars[i] != '*' || chars[i + 1] != '/') {
                continue;
            }
            int braceIndex = i + 2;
            while (braceIndex < lineEnd && (chars[braceIndex] == ' ' || chars[braceIndex] == '\t')) {
                braceIndex++;
            }
            if (braceIndex < lineEnd && chars[braceIndex] == '}') {
                return braceIndex;
            }
        }
        return -1;
    }

    private static void maskRange(char[] chars, int start, int endExclusive) {
        for (int i = start; i < endExclusive; i++) {
            if (chars[i] != '\n') {
                chars[i] = ' ';
            }
        }
    }

    private static int countRun(char[] chars, int index, int lineEnd, char expected) {
        if (index >= lineEnd || chars[index] != expected) {
            return 0;
        }
        int end = index;
        while (end < lineEnd && chars[end] == expected) {
            end++;
        }
        return end - index;
    }

    private static Fence detectFence(char[] chars, int lineStart, int lineEnd) {
        int i = lineStart;
        int spaces = 0;
        while (i < lineEnd && spaces < 4 && chars[i] == ' ') {
            i++;
            spaces++;
        }
        if (spaces > 3 || i >= lineEnd) {
            return null;
        }

        char marker = chars[i];
        if (marker != '`' && marker != '~') {
            return null;
        }

        int fenceLength = countRun(chars, i, lineEnd, marker);
        if (fenceLength < 3) {
            return null;
        }

        boolean closingFence = true;
        for (int j = i + fenceLength; j < lineEnd; j++) {
            if (chars[j] != ' ' && chars[j] != '\t') {
                closingFence = false;
                break;
            }
        }
        return new Fence(marker, fenceLength, closingFence);
    }

    private static final class Fence {

        private final char fenceChar;
        private final int fenceLength;
        private final boolean closingFence;

        private Fence(char fenceChar, int fenceLength, boolean closingFence) {
            this.fenceChar = fenceChar;
            this.fenceLength = fenceLength;
            this.closingFence = closingFence;
        }
    }
}
