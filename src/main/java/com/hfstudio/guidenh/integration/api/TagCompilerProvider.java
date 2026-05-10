package com.hfstudio.guidenh.integration.api;

import java.util.List;

import com.hfstudio.guidenh.guide.compiler.TagCompiler;

public interface TagCompilerProvider {

    void appendTagCompilers(List<TagCompiler> compilers);
}
