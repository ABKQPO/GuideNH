package com.hfstudio.guidenh.guide.scene.annotation;

/**
 * An in-world annotation that renders a flat grid on a horizontal plane (typically Y=0),
 * covering the structure's XZ footprint extended by one block in each direction.
 *
 * <p>
 * Grid lines are drawn at every integer X and Z coordinate within the specified bounds.
 */
public class SceneFloorGridAnnotation extends InWorldAnnotation {

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final float y;

    /**
     * @param minX inclusive grid start X (one block outside structure minX)
     * @param minZ inclusive grid start Z (one block outside structure minZ)
     * @param maxX inclusive grid end X (one block outside structure maxX+1)
     * @param maxZ inclusive grid end Z (one block outside structure maxZ+1)
     * @param y    world Y at which the grid plane is rendered
     */
    public SceneFloorGridAnnotation(int minX, int minZ, int maxX, int maxZ, float y) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.y = y;
        setAlwaysOnTop(false);
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public float getY() {
        return y;
    }
}
