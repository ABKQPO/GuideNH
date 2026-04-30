package com.hfstudio.guidenh.guide.internal.markdown;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class CodeBlockClipboardService {

    public void copy(String text) throws Exception {
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(text), null);
    }
}
