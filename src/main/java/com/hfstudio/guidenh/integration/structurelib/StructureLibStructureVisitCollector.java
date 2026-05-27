package com.hfstudio.guidenh.integration.structurelib;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureEvent.StructureElementVisitedEvent;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class StructureLibStructureVisitCollector {

    private final Object instrumentId;
    private final World world;
    private final Map<Long, IStructureElement<?>> visitedElementsByPos = new LinkedHashMap<>();

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
        visitedElementsByPos.put(pack(event.getX(), event.getY(), event.getZ()), event.getElement());
    }

    public Map<Long, IStructureElement<?>> snapshot() {
        if (visitedElementsByPos.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(visitedElementsByPos));
    }

    public static long pack(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }
}
