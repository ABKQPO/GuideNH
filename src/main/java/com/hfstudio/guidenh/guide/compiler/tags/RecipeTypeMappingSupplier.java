package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.function.Function;
import java.util.stream.Stream;

import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

public interface RecipeTypeMappingSupplier extends Extension {

    ExtensionPoint<RecipeTypeMappingSupplier> EXTENSION_POINT = new ExtensionPoint<>(RecipeTypeMappingSupplier.class);

    void collect(RecipeTypeMappings mappings);

    interface RecipeTypeMappings {

        void addFactory(String recipeTypeId, Function<Object, LytBlock> factory);

        void addStreamFactory(String recipeTypeId, Function<Object, Stream<? extends LytBlock>> factory);
    }
}
