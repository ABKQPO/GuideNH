package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.extensions.DefaultExtensions;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

final class SceneEditorTooltipCompiler {

    private static final String PREVIEW_SOURCE_PACK = "guidenh-scene-editor";
    private static final ResourceLocation PREVIEW_PAGE_ID = new ResourceLocation(
        "guidenh",
        "scene_editor/tooltip_preview");
    private static final ExtensionCollection EXTENSIONS = buildExtensions();
    private static final PreviewPageCollection PAGE_COLLECTION = new PreviewPageCollection();

    @Nullable
    GuideTooltip compile(@Nullable String tooltipMarkdown) {
        if (tooltipMarkdown == null || tooltipMarkdown.isEmpty()) {
            return null;
        }

        ParsedGuidePage parsed = PageCompiler.parse(PREVIEW_SOURCE_PACK, "en_us", PREVIEW_PAGE_ID, tooltipMarkdown);
        PageCompiler compiler = new PageCompiler(
            PAGE_COLLECTION,
            EXTENSIONS,
            parsed.getSourcePack(),
            parsed.getId(),
            parsed.getSource());
        LytVBox contentBox = new LytVBox();
        compiler.compileBlockContext(parsed.getAstRoot(), contentBox);
        if (contentBox.getChildren()
            .isEmpty()) {
            return new TextTooltip(tooltipMarkdown);
        }
        return new ContentTooltip(contentBox);
    }

    private static ExtensionCollection buildExtensions() {
        ExtensionCollection.Builder builder = ExtensionCollection.builder();
        DefaultExtensions.addAll(builder, Collections.emptySet());
        return builder.build();
    }

    private static final class PreviewPageCollection implements PageCollection {

        @Override
        public <T extends PageIndex> T getIndex(Class<T> indexClass) {
            for (MutableGuide guide : GuideRegistry.getAll()) {
                try {
                    return guide.getIndex(indexClass);
                } catch (IllegalArgumentException ignored) {}
            }
            throw new IllegalArgumentException("No index of type " + indexClass + " is available for tooltip preview.");
        }

        @Override
        public Collection<ParsedGuidePage> getPages() {
            Collection<MutableGuide> guides = GuideRegistry.getAll();
            if (guides.isEmpty()) {
                return Collections.emptyList();
            }
            ArrayList<ParsedGuidePage> pages = new ArrayList<>();
            for (MutableGuide guide : guides) {
                try {
                    pages.addAll(guide.getPages());
                } catch (IllegalStateException ignored) {}
            }
            return pages;
        }

        @Override
        @Nullable
        public ParsedGuidePage getParsedPage(ResourceLocation id) {
            for (MutableGuide guide : GuideRegistry.getAll()) {
                ParsedGuidePage page = guide.getParsedPage(id);
                if (page != null) {
                    return page;
                }
            }
            return null;
        }

        @Override
        @Nullable
        public GuidePage getPage(ResourceLocation id) {
            for (MutableGuide guide : GuideRegistry.getAll()) {
                if (!guide.pageExists(id)) {
                    continue;
                }
                GuidePage page = guide.getPage(id);
                if (page != null) {
                    return page;
                }
            }
            return null;
        }

        @Override
        @Nullable
        public byte[] loadAsset(ResourceLocation id) {
            return null;
        }

        @Override
        public NavigationTree getNavigationTree() {
            return new NavigationTree();
        }

        @Override
        public boolean pageExists(ResourceLocation pageId) {
            for (MutableGuide guide : GuideRegistry.getAll()) {
                if (guide.pageExists(pageId)) {
                    return true;
                }
            }
            return false;
        }
    }
}
