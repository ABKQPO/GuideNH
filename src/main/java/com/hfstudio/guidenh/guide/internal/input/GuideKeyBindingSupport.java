package com.hfstudio.guidenh.guide.internal.input;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuideKeyBindingSupport {

    private static final int MOUSE_KEY_OFFSET = 100;
    private static final int MOUSE_DISPLAY_OFFSET = 101;

    private GuideKeyBindingSupport() {}

    public static boolean isDown(@Nullable KeyBinding binding) {
        if (binding == null || binding.getKeyCode() == Keyboard.KEY_NONE) {
            return false;
        }
        try {
            return GameSettings.isKeyDown(binding);
        } catch (NoClassDefFoundError ignored) {
            return isRawKeyCodeDown(binding.getKeyCode());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static String describe(@Nullable KeyBinding binding) {
        return binding == null ? "" : describeKeyCode(binding.getKeyCode());
    }

    public static int mouseButtonKeyCode(int button) {
        return button - MOUSE_KEY_OFFSET;
    }

    public static boolean matchesKeyCode(@Nullable KeyBinding binding, int keyCode) {
        return binding != null && keyCode != Keyboard.KEY_NONE && binding.getKeyCode() == keyCode;
    }

    public static String describeKeyCode(int keyCode) {
        try {
            String display = GameSettings.getKeyDisplayString(keyCode);
            if (display != null && !display.isEmpty()) {
                return display;
            }
        } catch (NoClassDefFoundError ignored) {
            // Unit tests can run without Minecraft's client settings classes.
        } catch (RuntimeException ignored) {
            // Fall through to direct LWJGL names.
        }

        if (keyCode < 0) {
            return describeMouseButton(keyCode);
        }

        try {
            String keyboardName = Keyboard.getKeyName(keyCode);
            if (keyboardName != null && !keyboardName.isEmpty()) {
                return keyboardName;
            }
        } catch (NoClassDefFoundError | RuntimeException ignored) {
            // Fall through to the numeric fallback.
        }
        return Integer.toString(keyCode);
    }

    private static boolean isRawKeyCodeDown(int keyCode) {
        try {
            if (keyCode < 0) {
                return Mouse.isButtonDown(keyCode + MOUSE_KEY_OFFSET);
            }
            return Keyboard.isKeyDown(keyCode);
        } catch (NoClassDefFoundError | RuntimeException ignored) {
            return false;
        }
    }

    private static String describeMouseButton(int keyCode) {
        int button = keyCode + MOUSE_KEY_OFFSET;
        try {
            String mouseName = Mouse.getButtonName(button);
            if (mouseName != null && !mouseName.isEmpty()) {
                return mouseName;
            }
        } catch (NoClassDefFoundError | RuntimeException ignored) {
            // Fall through to the stable vanilla-style label.
        }
        return "Mouse Button " + (keyCode + MOUSE_DISPLAY_OFFSET);
    }
}
