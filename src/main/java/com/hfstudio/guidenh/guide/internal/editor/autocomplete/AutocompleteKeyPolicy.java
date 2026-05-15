package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import org.lwjgl.input.Keyboard;

public class AutocompleteKeyPolicy {

    private AutocompleteKeyPolicy() {}

    public static boolean shouldCloseForKey(char typedChar, int keyCode) {
        if (isModifierOnlyKey(keyCode)) {
            return false;
        }
        return typedChar >= 32 || keyCode == Keyboard.KEY_BACK
            || keyCode == Keyboard.KEY_DELETE
            || keyCode == Keyboard.KEY_LEFT
            || keyCode == Keyboard.KEY_RIGHT
            || keyCode == Keyboard.KEY_HOME
            || keyCode == Keyboard.KEY_END
            || keyCode == Keyboard.KEY_PRIOR
            || keyCode == Keyboard.KEY_NEXT
            || keyCode == Keyboard.KEY_SPACE;
    }

    private static boolean isModifierOnlyKey(int keyCode) {
        return keyCode == Keyboard.KEY_LCONTROL || keyCode == Keyboard.KEY_RCONTROL
            || keyCode == Keyboard.KEY_LSHIFT
            || keyCode == Keyboard.KEY_RSHIFT
            || keyCode == Keyboard.KEY_LMENU
            || keyCode == Keyboard.KEY_RMENU
            || keyCode == Keyboard.KEY_LMETA
            || keyCode == Keyboard.KEY_RMETA;
    }
}
