package com.hfstudio.guidenh.guide.internal.extensions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ATagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockImageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BoxFlowDirection;
import com.hfstudio.guidenh.guide.compiler.tags.BoxTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BreakCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.CategoryIndexCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ColorTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.CommandLinkCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.DivTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.FloatingImageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ItemImageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ItemLinkCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.KeyBindTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.PlayerNameTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.RecipeCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.StructureViewCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.SubPagesCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.TooltipTagCompiler;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.BlockAnnotationElementCompiler;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.BlockAnnotationTemplateElementCompiler;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.BoxAnnotationElementCompiler;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.DiamondAnnotationElementCompiler;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.LineAnnotationElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.BlockElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.EntityElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.ImportStructureElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.IsometricCameraElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.RemoveBlocksElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;

public final class DefaultExtensions {

    private static final List<Registration<?>> EXTENSIONS = Arrays.asList(
        new Registration<>(TagCompiler.EXTENSION_POINT, DefaultExtensions::tagCompilers),
        new Registration<>(SceneElementTagCompiler.EXTENSION_POINT, DefaultExtensions::sceneElementCompilers));

    private DefaultExtensions() {}

    public static void addAll(ExtensionCollection.Builder builder, Set<ExtensionPoint<?>> disabledExtensionPoints) {
        for (var registration : EXTENSIONS) {
            add(builder, disabledExtensionPoints, registration);
        }
    }

    private static <T extends Extension> void add(ExtensionCollection.Builder builder,
        Set<ExtensionPoint<?>> disabledExtensionPoints, Registration<T> registration) {
        if (disabledExtensionPoints.contains(registration.extensionPoint)) {
            return;
        }

        for (var extension : registration.factory.get()) {
            builder.add(registration.extensionPoint, extension);
        }
    }

    private static List<TagCompiler> tagCompilers() {
        return Arrays.asList(
            new DivTagCompiler(),
            new ATagCompiler(),
            new ColorTagCompiler(),
            new ItemLinkCompiler(),
            new FloatingImageCompiler(),
            new BreakCompiler(),
            new RecipeCompiler(),
            new ItemGridCompiler(),
            new CategoryIndexCompiler(),
            new BlockImageCompiler(),
            new ItemImageCompiler(),
            new BoxTagCompiler(BoxFlowDirection.ROW),
            new BoxTagCompiler(BoxFlowDirection.COLUMN),
            new SceneTagCompiler(),
            new SubPagesCompiler(),
            new CommandLinkCompiler(),
            new PlayerNameTagCompiler(),
            new KeyBindTagCompiler(),
            new TooltipTagCompiler(),
            new StructureViewCompiler());
    }

    private static List<SceneElementTagCompiler> sceneElementCompilers() {
        return Arrays.asList(
            new EntityElementCompiler(),
            new BlockElementCompiler(),
            new ImportStructureElementCompiler(),
            new IsometricCameraElementCompiler(),
            new BlockAnnotationElementCompiler(),
            new BoxAnnotationElementCompiler(),
            new LineAnnotationElementCompiler(),
            new DiamondAnnotationElementCompiler(),
            new BlockAnnotationTemplateElementCompiler(),
            new RemoveBlocksElementCompiler());
    }

    @Desugar
    private record Registration<T extends Extension> (ExtensionPoint<T> extensionPoint,
        Supplier<Collection<T>> factory) {}
}
