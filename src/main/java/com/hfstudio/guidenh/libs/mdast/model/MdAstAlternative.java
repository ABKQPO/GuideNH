package com.hfstudio.guidenh.libs.mdast.model;

import javax.annotation.Nullable;

/**
 * Represents a node with a fallback.
 * An alt field should be present. It represents equivalent content for environments that cannot represent the node as
 * intended.
 */
public interface MdAstAlternative {

    @Nullable
    String alt();
}
