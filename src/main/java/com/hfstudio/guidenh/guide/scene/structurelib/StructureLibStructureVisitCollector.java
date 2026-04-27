package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureEvent.StructureElementVisitedEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class StructureLibStructureVisitCollector {

    private final Object instrumentId;
    private final World world;
    private final List<StructureLibPreviewMetadataFactory.VisitedStructureElement> visitedElements = new ArrayList<>();

    public StructureLibStructureVisitCollector(Object instrumentId, World world) {
        this.instrumentId = instrumentId;
        this.world = world;
    }

    @SubscribeEvent
    public void onStructureElementVisited(StructureElementVisitedEvent event) {
        if (event == null || event.getElement() == null
            || event.getWorld() != world
            || !instrumentId.equals(event.getInstrumentIdentifier())) {
            return;
        }
        visitedElements.add(
            new StructureLibPreviewMetadataFactory.VisitedStructureElement(
                event.getX(),
                event.getY(),
                event.getZ(),
                event.getElement()));
    }

    public List<StructureLibPreviewMetadataFactory.VisitedStructureElement> snapshot() {
        if (visitedElements.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(visitedElements));
    }
}
