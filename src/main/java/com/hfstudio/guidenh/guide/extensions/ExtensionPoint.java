package com.hfstudio.guidenh.guide.extensions;

import com.github.bsideup.jabel.Desugar;

/**
 * An extension point is offered by the guidebook to plug in additional functionality. Each extension point defines an
 * interface or base-class that needs to be implemented (or extended) by an extension.
 */
@Desugar
public record ExtensionPoint<T extends Extension> (Class<T> extensionPointClass) {}
