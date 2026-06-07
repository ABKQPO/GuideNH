package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class KeyBindTagCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("KeyBind");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var id = getKeyBindId(el);
        if (id == null) {
            parent.appendError(compiler, "Attribute id or action is required.", el);
            return;
        }

        var placeholder = parent.appendText("");
        placeholder.setStyleClass("KeyBind");
        placeholder.setStyle(LytParagraph.PLACEHOLDER_STYLE);
        placeholder.setText("[KeyBind]");
        placeholder.setData("bindId", id);
    }

    public static String getKeyBindId(MdxJsxElementFields el) {
        if (el == null) {
            return null;
        }
        String id = el.getAttributeString("id", null);
        if (id == null || id.trim()
            .isEmpty()) {
            id = el.getAttributeString("action", null);
        }
        if (id == null) {
            return null;
        }
        id = id.trim();
        return id.isEmpty() ? null : id;
    }

    // --- Static helpers used by external callers (e.g. site exporter) ---

    public static KeyBinding findMapping(String id) {
        return findMapping(Minecraft.getMinecraft().gameSettings.keyBindings, id);
    }

    public static KeyBinding findMapping(KeyBinding[] keyMappings, String id) {
        for (var keyMapping : keyMappings) {
            if (matchesId(keyMapping, id)) {
                return keyMapping;
            }
        }
        return null;
    }

    public static String describeMapping(KeyBinding mapping) {
        try {
            String display = GameSettings.getKeyDisplayString(mapping.getKeyCode());
            if (display != null && !display.isEmpty()) {
                return display;
            }
        } catch (NoClassDefFoundError ignored) {
            // Unit tests can run without the client input classes on the runtime classpath.
        }

        try {
            String keyboardName = Keyboard.getKeyName(mapping.getKeyCode());
            if (keyboardName != null && !keyboardName.isEmpty()) {
                return keyboardName;
            }
        } catch (NoClassDefFoundError ignored) {
            // Fall through to the numeric keycode fallback below.
        }

        return Integer.toString(mapping.getKeyCode());
    }

    private static boolean matchesId(KeyBinding keyMapping, String id) {
        return id.equals(keyMapping.getKeyDescription())
            || id.equals(keyMapping.getKeyCategory() + "." + keyMapping.getKeyDescription());
    }
}
