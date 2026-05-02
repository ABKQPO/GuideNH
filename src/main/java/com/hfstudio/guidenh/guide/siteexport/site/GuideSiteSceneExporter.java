package com.hfstudio.guidenh.guide.siteexport.site;

public class GuideSiteSceneExporter {

    public interface BytesProducer {

        byte[] produce() throws Exception;
    }

    public static final class SceneFiles {

        private final String placeholderPath;
        private final String scenePath;

        public SceneFiles(String placeholderPath, String scenePath) {
            this.placeholderPath = placeholderPath;
            this.scenePath = scenePath;
        }

        public String placeholderPath() {
            return placeholderPath;
        }

        public String scenePath() {
            return scenePath;
        }
    }

    private final GuideSiteAssetRegistry assets;
    private final BytesProducer placeholderProducer;
    private final BytesProducer sceneProducer;

    public GuideSiteSceneExporter(GuideSiteAssetRegistry assets, BytesProducer placeholderProducer,
        BytesProducer sceneProducer) {
        this.assets = assets;
        this.placeholderProducer = placeholderProducer;
        this.sceneProducer = sceneProducer;
    }

    public SceneFiles writeSceneAssets() throws Exception {
        String placeholder = assets.writeShared("placeholders", ".png", placeholderProducer.produce());
        String scene = assets.writeShared("scenes", ".scene.gz", sceneProducer.produce());
        return new SceneFiles(placeholder, scene);
    }
}
