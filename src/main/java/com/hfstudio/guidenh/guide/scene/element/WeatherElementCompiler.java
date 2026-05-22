package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneWeatherEffect;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneWeatherSupport;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneWeatherType;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class WeatherElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Weather");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        LytGuidebookScene scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "Weather tag used outside <GameScene>", el);
            return;
        }

        String weatherName = MdxAttrs.getString(compiler, errorSink, el, "weather", null);
        if (weatherName == null || weatherName.trim()
            .isEmpty()) {
            weatherName = MdxAttrs.getString(compiler, errorSink, el, "type", "rain");
        }
        GuidebookSceneWeatherType weatherType = GuidebookSceneWeatherType.fromSerializedName(weatherName);
        int[] xValues = GuidebookSceneWeatherSupport
            .parseAxisValues(MdxAttrs.getString(compiler, errorSink, el, "x", null));
        int[] zValues = GuidebookSceneWeatherSupport
            .parseAxisValues(MdxAttrs.getString(compiler, errorSink, el, "z", null));
        int density = Math.max(
            1,
            MdxAttrs
                .getInt(compiler, errorSink, el, "density", GuidebookSceneWeatherSupport.defaultDensity(weatherType)));

        scene.addStaticWeatherEffect(
            new GuidebookSceneWeatherEffect(
                weatherType,
                xValues,
                zValues,
                0,
                GuidebookSceneWeatherEffect.INFINITE_DURATION,
                density,
                false));
    }
}
