package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

@Getter
public class PonderKeyframeTileNbtOperation {

    private int x;
    private int y;
    private int z;
    @Nullable
    private String nbt;
    @Nullable
    private String path;
    @Nullable
    private String value;

}
