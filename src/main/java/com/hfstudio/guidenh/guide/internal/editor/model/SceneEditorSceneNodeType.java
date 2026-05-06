package com.hfstudio.guidenh.guide.internal.editor.model;

public enum SceneEditorSceneNodeType {
    IMPORT_STRUCTURE,
    IMPORT_STRUCTURE_LIB,
    REMOVE_BLOCKS,
    BLOCK_ANNOTATION_TEMPLATE,
    ANNOTATION,
    /** Raw MDX text preserved verbatim — for tags the editor doesn't recognise but GameScene does. */
    OPAQUE
}
