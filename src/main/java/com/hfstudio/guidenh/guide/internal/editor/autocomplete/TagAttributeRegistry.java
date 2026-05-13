package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import java.util.*;

public final class TagAttributeRegistry {

    private static final Map<String, List<AttributeSpec>> registry = new LinkedHashMap<>();

    private TagAttributeRegistry() {}

    public static void register(String tagName, AttributeSpec... specs) {
        registry.computeIfAbsent(tagName, k -> new ArrayList<>())
            .addAll(Arrays.asList(specs));
    }

    public static List<AttributeSpec> get(String tagName) {
        return Collections.unmodifiableList(
            registry.getOrDefault(tagName, Collections.emptyList()));
    }

    public static Set<String> getRegisteredTags() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /** Populate the registry with all known tag→attribute mappings. */
    public static void initialize() {
        // Inline/Flow tags
        register("ItemImage",
            new AttributeSpec("id", AttrType.ITEM_ID),
            new AttributeSpec("ore", AttrType.ORE_DICT),
            new AttributeSpec("scale", AttrType.FLOAT),
            new AttributeSpec("yOffset", AttrType.INT),
            new AttributeSpec("labelYOffset", AttrType.INT),
            new AttributeSpec("noTooltip", AttrType.BOOLEAN),
            new AttributeSpec("showTooltip", AttrType.BOOLEAN),
            new AttributeSpec("showIcon", AttrType.STRING),
            new AttributeSpec("label", AttrType.STRING),
            new AttributeSpec("format", AttrType.FORMAT_PATTERN));
        register("ItemLink",
            new AttributeSpec("id", AttrType.ITEM_ID),
            new AttributeSpec("ore", AttrType.ORE_DICT),
            new AttributeSpec("noTooltip", AttrType.BOOLEAN),
            new AttributeSpec("showTooltip", AttrType.BOOLEAN),
            new AttributeSpec("showIcon", AttrType.STRING),
            new AttributeSpec("linksTo", AttrType.PAGE_PATH));
        register("BlockImage",
            new AttributeSpec("id", AttrType.BLOCK_ID),
            new AttributeSpec("ore", AttrType.ORE_DICT),
            new AttributeSpec("scale", AttrType.FLOAT),
            new AttributeSpec("wrap", AttrType.STRING),
            new AttributeSpec("align", AttrType.STRING),
            new AttributeSpec("float", AttrType.STRING));
        register("FloatingImage",
            new AttributeSpec("src", AttrType.FILE_PATH),
            new AttributeSpec("align", AttrType.STRING),
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT));
        register("Color",
            new AttributeSpec("id", AttrType.COLOR),
            new AttributeSpec("color", AttrType.COLOR));
        register("KeyBind",
            new AttributeSpec("id", AttrType.KEY_BIND),
            new AttributeSpec("action", AttrType.STRING));
        register("CommandLink",
            new AttributeSpec("command", AttrType.COMMAND),
            new AttributeSpec("close", AttrType.BOOLEAN),
            new AttributeSpec("title", AttrType.STRING));
        register("Recipe",
            new AttributeSpec("id", AttrType.ITEM_ID),
            new AttributeSpec("fallbackText", AttrType.STRING),
            new AttributeSpec("handlerName", AttrType.STRING),
            new AttributeSpec("handlerId", AttrType.STRING),
            new AttributeSpec("handlerOrder", AttrType.INT),
            new AttributeSpec("input", AttrType.STRING),
            new AttributeSpec("output", AttrType.STRING),
            new AttributeSpec("limit", AttrType.INT));
        register("RecipeFor",
            new AttributeSpec("id", AttrType.ITEM_ID),
            new AttributeSpec("fallbackText", AttrType.STRING),
            new AttributeSpec("handlerName", AttrType.STRING),
            new AttributeSpec("handlerId", AttrType.STRING),
            new AttributeSpec("handlerOrder", AttrType.INT),
            new AttributeSpec("input", AttrType.STRING),
            new AttributeSpec("output", AttrType.STRING),
            new AttributeSpec("limit", AttrType.INT));
        register("RecipesFor",
            new AttributeSpec("id", AttrType.ITEM_ID),
            new AttributeSpec("fallbackText", AttrType.STRING),
            new AttributeSpec("handlerName", AttrType.STRING),
            new AttributeSpec("handlerId", AttrType.STRING),
            new AttributeSpec("handlerOrder", AttrType.INT),
            new AttributeSpec("input", AttrType.STRING),
            new AttributeSpec("output", AttrType.STRING),
            new AttributeSpec("limit", AttrType.INT));
        register("SubPages",
            new AttributeSpec("id", AttrType.PAGE_PATH),
            new AttributeSpec("alphabetical", AttrType.BOOLEAN));
        register("CategoryIndex",
            new AttributeSpec("category", AttrType.STRING));
        register("Structure",
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT));
        register("Mermaid",
            new AttributeSpec("src", AttrType.FILE_PATH),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT));
        register("CsvTable",
            new AttributeSpec("src", AttrType.FILE_PATH),
            new AttributeSpec("header", AttrType.BOOLEAN),
            new AttributeSpec("widths", AttrType.STRING));
        register("details",
            new AttributeSpec("open", AttrType.BOOLEAN));
        register("Row",
            new AttributeSpec("gap", AttrType.INT),
            new AttributeSpec("alignItems", AttrType.STRING),
            new AttributeSpec("fullWidth", AttrType.BOOLEAN),
            new AttributeSpec("width", AttrType.INT));
        register("Column",
            new AttributeSpec("gap", AttrType.INT),
            new AttributeSpec("alignItems", AttrType.STRING),
            new AttributeSpec("fullWidth", AttrType.BOOLEAN),
            new AttributeSpec("width", AttrType.INT));
        register("a",
            new AttributeSpec("name", AttrType.STRING),
            new AttributeSpec("href", AttrType.PAGE_PATH),
            new AttributeSpec("title", AttrType.STRING));
        register("br",
            new AttributeSpec("clear", AttrType.STRING));
        register("ImportPonder",
            new AttributeSpec("src", AttrType.FILE_PATH));
        register("RemoveBlocks",
            new AttributeSpec("id", AttrType.BLOCK_ID));
        register("IsometricCamera",
            new AttributeSpec("yaw", AttrType.FLOAT),
            new AttributeSpec("pitch", AttrType.FLOAT),
            new AttributeSpec("roll", AttrType.FLOAT));
        register("Tooltip",
            new AttributeSpec("label", AttrType.STRING));
        register("mark",
            new AttributeSpec("color", AttrType.COLOR));
        register("FileTree",
            new AttributeSpec("indent", AttrType.INT),
            new AttributeSpec("gap", AttrType.INT));
        register("ItemGrid"); // no attributes - uses child elements
        register("FootnoteList",
            new AttributeSpec("width", AttrType.INT));

        // === Charts (all five types share CommonChartAttrs) ===
        register("BarChart",
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("background", AttrType.COLOR),
            new AttributeSpec("border", AttrType.COLOR),
            new AttributeSpec("titleColor", AttrType.COLOR),
            new AttributeSpec("labelColor", AttrType.COLOR),
            new AttributeSpec("legend", AttrType.STRING),
            new AttributeSpec("labelPosition", AttrType.STRING),
            new AttributeSpec("cornerLegend", AttrType.STRING),
            new AttributeSpec("cornerLegendWidth", AttrType.INT),
            new AttributeSpec("cornerLegendHeight", AttrType.INT),
            new AttributeSpec("cornerLegendBackground", AttrType.COLOR),
            new AttributeSpec("categories", AttrType.STRING),
            new AttributeSpec("xAxisLabel", AttrType.STRING),
            new AttributeSpec("yAxisLabel", AttrType.STRING),
            new AttributeSpec("xAxisMin", AttrType.FLOAT),
            new AttributeSpec("xAxisMax", AttrType.FLOAT),
            new AttributeSpec("yAxisMin", AttrType.FLOAT),
            new AttributeSpec("yAxisMax", AttrType.FLOAT),
            new AttributeSpec("xAxisStep", AttrType.FLOAT),
            new AttributeSpec("yAxisStep", AttrType.FLOAT),
            new AttributeSpec("xAxisUnit", AttrType.STRING),
            new AttributeSpec("yAxisUnit", AttrType.STRING),
            new AttributeSpec("xAxisTickFormat", AttrType.STRING),
            new AttributeSpec("yAxisTickFormat", AttrType.STRING),
            new AttributeSpec("showXGrid", AttrType.BOOLEAN),
            new AttributeSpec("showYGrid", AttrType.BOOLEAN),
            new AttributeSpec("xGridColor", AttrType.COLOR),
            new AttributeSpec("yGridColor", AttrType.COLOR),
            new AttributeSpec("barWidthRatio", AttrType.FLOAT));
        register("ColumnChart",
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("background", AttrType.COLOR),
            new AttributeSpec("border", AttrType.COLOR),
            new AttributeSpec("titleColor", AttrType.COLOR),
            new AttributeSpec("labelColor", AttrType.COLOR),
            new AttributeSpec("legend", AttrType.STRING),
            new AttributeSpec("labelPosition", AttrType.STRING),
            new AttributeSpec("cornerLegend", AttrType.STRING),
            new AttributeSpec("cornerLegendWidth", AttrType.INT),
            new AttributeSpec("cornerLegendHeight", AttrType.INT),
            new AttributeSpec("cornerLegendBackground", AttrType.COLOR),
            new AttributeSpec("categories", AttrType.STRING),
            new AttributeSpec("xAxisLabel", AttrType.STRING),
            new AttributeSpec("yAxisLabel", AttrType.STRING),
            new AttributeSpec("xAxisMin", AttrType.FLOAT),
            new AttributeSpec("xAxisMax", AttrType.FLOAT),
            new AttributeSpec("yAxisMin", AttrType.FLOAT),
            new AttributeSpec("yAxisMax", AttrType.FLOAT),
            new AttributeSpec("xAxisStep", AttrType.FLOAT),
            new AttributeSpec("yAxisStep", AttrType.FLOAT),
            new AttributeSpec("xAxisUnit", AttrType.STRING),
            new AttributeSpec("yAxisUnit", AttrType.STRING),
            new AttributeSpec("xAxisTickFormat", AttrType.STRING),
            new AttributeSpec("yAxisTickFormat", AttrType.STRING),
            new AttributeSpec("showXGrid", AttrType.BOOLEAN),
            new AttributeSpec("showYGrid", AttrType.BOOLEAN),
            new AttributeSpec("xGridColor", AttrType.COLOR),
            new AttributeSpec("yGridColor", AttrType.COLOR),
            new AttributeSpec("barWidthRatio", AttrType.FLOAT));
        register("LineChart",
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("background", AttrType.COLOR),
            new AttributeSpec("border", AttrType.COLOR),
            new AttributeSpec("titleColor", AttrType.COLOR),
            new AttributeSpec("labelColor", AttrType.COLOR),
            new AttributeSpec("legend", AttrType.STRING),
            new AttributeSpec("labelPosition", AttrType.STRING),
            new AttributeSpec("cornerLegend", AttrType.STRING),
            new AttributeSpec("cornerLegendWidth", AttrType.INT),
            new AttributeSpec("cornerLegendHeight", AttrType.INT),
            new AttributeSpec("cornerLegendBackground", AttrType.COLOR),
            new AttributeSpec("categories", AttrType.STRING),
            new AttributeSpec("xAxisLabel", AttrType.STRING),
            new AttributeSpec("yAxisLabel", AttrType.STRING),
            new AttributeSpec("xAxisMin", AttrType.FLOAT),
            new AttributeSpec("xAxisMax", AttrType.FLOAT),
            new AttributeSpec("yAxisMin", AttrType.FLOAT),
            new AttributeSpec("yAxisMax", AttrType.FLOAT),
            new AttributeSpec("xAxisStep", AttrType.FLOAT),
            new AttributeSpec("yAxisStep", AttrType.FLOAT),
            new AttributeSpec("xAxisUnit", AttrType.STRING),
            new AttributeSpec("yAxisUnit", AttrType.STRING),
            new AttributeSpec("xAxisTickFormat", AttrType.STRING),
            new AttributeSpec("yAxisTickFormat", AttrType.STRING),
            new AttributeSpec("showXGrid", AttrType.BOOLEAN),
            new AttributeSpec("showYGrid", AttrType.BOOLEAN),
            new AttributeSpec("xGridColor", AttrType.COLOR),
            new AttributeSpec("yGridColor", AttrType.COLOR),
            new AttributeSpec("numericX", AttrType.BOOLEAN),
            new AttributeSpec("showPoints", AttrType.BOOLEAN));
        register("ScatterChart",
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("background", AttrType.COLOR),
            new AttributeSpec("border", AttrType.COLOR),
            new AttributeSpec("titleColor", AttrType.COLOR),
            new AttributeSpec("labelColor", AttrType.COLOR),
            new AttributeSpec("legend", AttrType.STRING),
            new AttributeSpec("labelPosition", AttrType.STRING),
            new AttributeSpec("cornerLegend", AttrType.STRING),
            new AttributeSpec("cornerLegendWidth", AttrType.INT),
            new AttributeSpec("cornerLegendHeight", AttrType.INT),
            new AttributeSpec("cornerLegendBackground", AttrType.COLOR),
            new AttributeSpec("xAxisLabel", AttrType.STRING),
            new AttributeSpec("yAxisLabel", AttrType.STRING),
            new AttributeSpec("xAxisMin", AttrType.FLOAT),
            new AttributeSpec("xAxisMax", AttrType.FLOAT),
            new AttributeSpec("yAxisMin", AttrType.FLOAT),
            new AttributeSpec("yAxisMax", AttrType.FLOAT),
            new AttributeSpec("xAxisStep", AttrType.FLOAT),
            new AttributeSpec("yAxisStep", AttrType.FLOAT),
            new AttributeSpec("xAxisUnit", AttrType.STRING),
            new AttributeSpec("yAxisUnit", AttrType.STRING),
            new AttributeSpec("xAxisTickFormat", AttrType.STRING),
            new AttributeSpec("yAxisTickFormat", AttrType.STRING),
            new AttributeSpec("showXGrid", AttrType.BOOLEAN),
            new AttributeSpec("showYGrid", AttrType.BOOLEAN),
            new AttributeSpec("xGridColor", AttrType.COLOR),
            new AttributeSpec("yGridColor", AttrType.COLOR));
        register("PieChart",
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("background", AttrType.COLOR),
            new AttributeSpec("border", AttrType.COLOR),
            new AttributeSpec("titleColor", AttrType.COLOR),
            new AttributeSpec("labelColor", AttrType.COLOR),
            new AttributeSpec("legend", AttrType.STRING),
            new AttributeSpec("labelPosition", AttrType.STRING),
            new AttributeSpec("cornerLegend", AttrType.STRING),
            new AttributeSpec("cornerLegendWidth", AttrType.INT),
            new AttributeSpec("cornerLegendHeight", AttrType.INT),
            new AttributeSpec("cornerLegendBackground", AttrType.COLOR),
            new AttributeSpec("startAngle", AttrType.FLOAT),
            new AttributeSpec("clockwise", AttrType.BOOLEAN));

        // === Chart child tags ===
        register("Series",
            new AttributeSpec("name", AttrType.STRING),
            new AttributeSpec("data", AttrType.STRING),
            new AttributeSpec("points", AttrType.STRING),
            new AttributeSpec("color", AttrType.COLOR),
            new AttributeSpec("icon", AttrType.ITEM_ID),
            new AttributeSpec("iconImage", AttrType.FILE_PATH),
            new AttributeSpec("tooltip", AttrType.STRING));
        register("LineSeries",
            new AttributeSpec("name", AttrType.STRING),
            new AttributeSpec("data", AttrType.STRING),
            new AttributeSpec("color", AttrType.COLOR),
            new AttributeSpec("icon", AttrType.ITEM_ID),
            new AttributeSpec("iconImage", AttrType.FILE_PATH),
            new AttributeSpec("tooltip", AttrType.STRING));
        register("Slice",
            new AttributeSpec("label", AttrType.STRING),
            new AttributeSpec("value", AttrType.FLOAT),
            new AttributeSpec("color", AttrType.COLOR),
            new AttributeSpec("icon", AttrType.ITEM_ID),
            new AttributeSpec("iconImage", AttrType.FILE_PATH),
            new AttributeSpec("tooltip", AttrType.STRING));
        register("PieInset",
            new AttributeSpec("size", AttrType.FLOAT),
            new AttributeSpec("position", AttrType.STRING),
            new AttributeSpec("startAngleDeg", AttrType.FLOAT),
            new AttributeSpec("direction", AttrType.STRING),
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("titleColor", AttrType.COLOR));

        // === FunctionGraph child tags ===
        register("Plot",
            new AttributeSpec("expr", AttrType.EXPRESSION),
            new AttributeSpec("inverse", AttrType.BOOLEAN),
            new AttributeSpec("domain", AttrType.DOMAIN),
            new AttributeSpec("color", AttrType.COLOR),
            new AttributeSpec("label", AttrType.STRING),
            new AttributeSpec("pointEveryX", AttrType.FLOAT),
            new AttributeSpec("pointEveryY", AttrType.FLOAT),
            new AttributeSpec("autoPointLabel", AttrType.STRING),
            new AttributeSpec("autoPointColor", AttrType.COLOR));
        register("Point",
            new AttributeSpec("x", AttrType.FLOAT),
            new AttributeSpec("y", AttrType.FLOAT),
            new AttributeSpec("color", AttrType.COLOR),
            new AttributeSpec("label", AttrType.STRING),
            new AttributeSpec("plot", AttrType.INT),
            new AttributeSpec("atX", AttrType.FLOAT),
            new AttributeSpec("atY", AttrType.FLOAT));

        // === Fix and extend existing registrations ===

        // GameScene: add camera attributes
        register("Scene",
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("zoom", AttrType.FLOAT),
            new AttributeSpec("perspective", AttrType.STRING),
            new AttributeSpec("rotateX", AttrType.FLOAT),
            new AttributeSpec("rotateY", AttrType.FLOAT),
            new AttributeSpec("rotateZ", AttrType.FLOAT),
            new AttributeSpec("offsetX", AttrType.FLOAT),
            new AttributeSpec("offsetY", AttrType.FLOAT),
            new AttributeSpec("centerX", AttrType.FLOAT),
            new AttributeSpec("centerY", AttrType.FLOAT),
            new AttributeSpec("centerZ", AttrType.FLOAT),
            new AttributeSpec("interactive", AttrType.BOOLEAN),
            new AttributeSpec("allowLayerSlider", AttrType.BOOLEAN),
            new AttributeSpec("gridButtonEnabled", AttrType.BOOLEAN),
            new AttributeSpec("showGrid", AttrType.BOOLEAN));

        // GameScene: also register as "GameScene" with same attrs (SceneTagCompiler handles both)
        register("GameScene",
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("zoom", AttrType.FLOAT),
            new AttributeSpec("perspective", AttrType.STRING),
            new AttributeSpec("rotateX", AttrType.FLOAT),
            new AttributeSpec("rotateY", AttrType.FLOAT),
            new AttributeSpec("rotateZ", AttrType.FLOAT),
            new AttributeSpec("offsetX", AttrType.FLOAT),
            new AttributeSpec("offsetY", AttrType.FLOAT),
            new AttributeSpec("centerX", AttrType.FLOAT),
            new AttributeSpec("centerY", AttrType.FLOAT),
            new AttributeSpec("centerZ", AttrType.FLOAT),
            new AttributeSpec("interactive", AttrType.BOOLEAN),
            new AttributeSpec("allowLayerSlider", AttrType.BOOLEAN),
            new AttributeSpec("gridButtonEnabled", AttrType.BOOLEAN),
            new AttributeSpec("showGrid", AttrType.BOOLEAN));

        // Function: add missing container + plot attrs
        register("Function",
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("background", AttrType.COLOR),
            new AttributeSpec("border", AttrType.COLOR),
            new AttributeSpec("axisColor", AttrType.COLOR),
            new AttributeSpec("gridColor", AttrType.COLOR),
            new AttributeSpec("showGrid", AttrType.BOOLEAN),
            new AttributeSpec("showAxes", AttrType.BOOLEAN),
            new AttributeSpec("xMin", AttrType.FLOAT),
            new AttributeSpec("xMax", AttrType.FLOAT),
            new AttributeSpec("yMin", AttrType.FLOAT),
            new AttributeSpec("yMax", AttrType.FLOAT),
            new AttributeSpec("xStep", AttrType.FLOAT),
            new AttributeSpec("yStep", AttrType.FLOAT),
            new AttributeSpec("xRange", AttrType.STRING),
            new AttributeSpec("yRange", AttrType.STRING),
            new AttributeSpec("quadrants", AttrType.STRING),
            new AttributeSpec("cornerLegend", AttrType.STRING),
            new AttributeSpec("cornerLegendWidth", AttrType.INT),
            new AttributeSpec("cornerLegendHeight", AttrType.INT),
            new AttributeSpec("cornerLegendBackground", AttrType.COLOR),
            new AttributeSpec("expr", AttrType.EXPRESSION),
            new AttributeSpec("inverse", AttrType.BOOLEAN),
            new AttributeSpec("domain", AttrType.DOMAIN),
            new AttributeSpec("color", AttrType.COLOR),
            new AttributeSpec("label", AttrType.STRING),
            new AttributeSpec("pointEveryX", AttrType.FLOAT),
            new AttributeSpec("pointEveryY", AttrType.FLOAT),
            new AttributeSpec("autoPointLabel", AttrType.STRING),
            new AttributeSpec("autoPointColor", AttrType.COLOR));

        // FunctionGraph: add missing container attrs
        register("FunctionGraph",
            new AttributeSpec("title", AttrType.STRING),
            new AttributeSpec("width", AttrType.INT),
            new AttributeSpec("height", AttrType.INT),
            new AttributeSpec("background", AttrType.COLOR),
            new AttributeSpec("border", AttrType.COLOR),
            new AttributeSpec("axisColor", AttrType.COLOR),
            new AttributeSpec("gridColor", AttrType.COLOR),
            new AttributeSpec("showGrid", AttrType.BOOLEAN),
            new AttributeSpec("showAxes", AttrType.BOOLEAN),
            new AttributeSpec("xMin", AttrType.FLOAT),
            new AttributeSpec("xMax", AttrType.FLOAT),
            new AttributeSpec("yMin", AttrType.FLOAT),
            new AttributeSpec("yMax", AttrType.FLOAT),
            new AttributeSpec("xStep", AttrType.FLOAT),
            new AttributeSpec("yStep", AttrType.FLOAT),
            new AttributeSpec("xRange", AttrType.STRING),
            new AttributeSpec("yRange", AttrType.STRING),
            new AttributeSpec("quadrants", AttrType.STRING),
            new AttributeSpec("cornerLegend", AttrType.STRING),
            new AttributeSpec("cornerLegendWidth", AttrType.INT),
            new AttributeSpec("cornerLegendHeight", AttrType.INT),
            new AttributeSpec("cornerLegendBackground", AttrType.COLOR));

        // Entity: add missing rotation attrs
        register("Entity",
            new AttributeSpec("id", AttrType.ENTITY_ID),
            new AttributeSpec("data", AttrType.SNBT),
            new AttributeSpec("name", AttrType.STRING),
            new AttributeSpec("uuid", AttrType.STRING),
            new AttributeSpec("showName", AttrType.BOOLEAN),
            new AttributeSpec("showCape", AttrType.BOOLEAN),
            new AttributeSpec("baby", AttrType.BOOLEAN),
            new AttributeSpec("x", AttrType.FLOAT),
            new AttributeSpec("y", AttrType.FLOAT),
            new AttributeSpec("z", AttrType.FLOAT),
            new AttributeSpec("rotationY", AttrType.FLOAT),
            new AttributeSpec("rotationX", AttrType.FLOAT),
            new AttributeSpec("headRotation", AttrType.STRING),
            new AttributeSpec("leftArmRotation", AttrType.STRING),
            new AttributeSpec("rightArmRotation", AttrType.STRING),
            new AttributeSpec("leftLegRotation", AttrType.STRING),
            new AttributeSpec("rightLegRotation", AttrType.STRING),
            new AttributeSpec("capeRotation", AttrType.STRING));

        // Latex: add missing attrs
        register("Latex",
            new AttributeSpec("formula", AttrType.STRING),
            new AttributeSpec("color", AttrType.COLOR),
            new AttributeSpec("scale", AttrType.FLOAT),
            new AttributeSpec("sourceScale", AttrType.FLOAT),
            new AttributeSpec("showTooltip", AttrType.BOOLEAN),
            new AttributeSpec("valign", AttrType.STRING),
            new AttributeSpec("offsetX", AttrType.INT),
            new AttributeSpec("offsetY", AttrType.INT));

        // ImportStructureLib: add offset attrs
        register("ImportStructureLib",
            new AttributeSpec("controller", AttrType.STRING),
            new AttributeSpec("piece", AttrType.STRING),
            new AttributeSpec("channel", AttrType.STRING),
            new AttributeSpec("facing", AttrType.STRING),
            new AttributeSpec("rotation", AttrType.STRING),
            new AttributeSpec("flip", AttrType.STRING),
            new AttributeSpec("offsetX", AttrType.INT),
            new AttributeSpec("offsetY", AttrType.INT),
            new AttributeSpec("offsetZ", AttrType.INT));

        // PlaceBlock: add dx/dy/dz
        register("PlaceBlock",
            new AttributeSpec("id", AttrType.BLOCK_ID),
            new AttributeSpec("nbt", AttrType.SNBT),
            new AttributeSpec("x", AttrType.INT),
            new AttributeSpec("y", AttrType.INT),
            new AttributeSpec("z", AttrType.INT),
            new AttributeSpec("dx", AttrType.INT),
            new AttributeSpec("dy", AttrType.INT),
            new AttributeSpec("dz", AttrType.INT));

        // ReplaceBlock: add bounds attrs
        register("ReplaceBlock",
            new AttributeSpec("from", AttrType.BLOCK_ID),
            new AttributeSpec("to", AttrType.BLOCK_ID),
            new AttributeSpec("from_nbt", AttrType.SNBT),
            new AttributeSpec("to_nbt", AttrType.SNBT),
            new AttributeSpec("x", AttrType.INT),
            new AttributeSpec("y", AttrType.INT),
            new AttributeSpec("z", AttrType.INT),
            new AttributeSpec("dx", AttrType.INT),
            new AttributeSpec("dy", AttrType.INT),
            new AttributeSpec("dz", AttrType.INT));

        // ImportStructure: fix types + add x/y/z
        register("ImportStructure",
            new AttributeSpec("src", AttrType.FILE_PATH),
            new AttributeSpec("x", AttrType.INT),
            new AttributeSpec("y", AttrType.INT),
            new AttributeSpec("z", AttrType.INT),
            new AttributeSpec("offsetX", AttrType.INT),
            new AttributeSpec("offsetY", AttrType.INT),
            new AttributeSpec("offsetZ", AttrType.INT));

        // Block scene element
        register("Block",
            new AttributeSpec("id", AttrType.BLOCK_ID),
            new AttributeSpec("ore", AttrType.ORE_DICT),
            new AttributeSpec("x", AttrType.INT),
            new AttributeSpec("y", AttrType.INT),
            new AttributeSpec("z", AttrType.INT),
            new AttributeSpec("meta", AttrType.INT),
            new AttributeSpec("facing", AttrType.STRING),
            new AttributeSpec("nbt", AttrType.SNBT));
    }
}
