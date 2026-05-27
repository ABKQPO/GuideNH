package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.SceneSoundCue;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneConditionParser;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.sound.GuideSoundParsers;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class PlaySoundElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("PlaySound");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        LytGuidebookScene scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "PlaySound used outside <GameScene>", el);
            return;
        }
        var sound = GuideSoundParsers.parseAttributes(compiler, errorSink, el);
        if (sound == null) {
            errorSink.appendError(compiler, "PlaySound requires a sound or src attribute.", el);
            return;
        }
        var trigger = GuideSoundTrigger
            .parse(MdxAttrs.getString(compiler, errorSink, el, "trigger", null), GuideSoundTrigger.CLICK);
        SceneSoundCue cue = new SceneSoundCue(trigger, sound);
        cue.setStructureLibCondition(StructureLibSceneConditionParser.parse(compiler, errorSink, el));
        scene.addSoundCue(cue);
    }
}
