package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class KeyBindTagCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("KeyBind");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var id = el.getAttributeString("id", null);
        if (id == null) {
            parent.appendError(compiler, "Attribute id is required.", el);
            return;
        }

        var mapping = findMapping(id);
        if (mapping == null) {
            parent.appendError(compiler, "No key mapping with this id was found.", el);
            return;
        }

        parent.appendText(mapping.getKeyDescription());
    }

    private static KeyBinding findMapping(String id) {
        var keyMappings = Minecraft.getMinecraft().gameSettings.keyBindings;
        for (var keyMapping : keyMappings) {
            if (id.equals(keyMapping.getKeyCategory() + "." + keyMapping.getKeyDescription())
                || id.equals(keyMapping.getKeyDescription())) {
                return keyMapping;
            }
        }
        return null;
    }
}
