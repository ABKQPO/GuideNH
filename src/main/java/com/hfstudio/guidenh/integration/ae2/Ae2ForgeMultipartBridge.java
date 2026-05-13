package com.hfstudio.guidenh.integration.ae2;

import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.Mods;

import appeng.fmp.CableBusPart;
import appeng.parts.CableBusContainer;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import cpw.mods.fml.common.Optional;

final class Ae2ForgeMultipartBridge {

    private Ae2ForgeMultipartBridge() {}

    @Optional.Method(modid = "ForgeMultipart")
    @Nullable
    static CableBusContainer resolveCableContainer(@Nullable TileEntity tileEntity) {
        if (!Mods.ForgeMultipart.isModLoaded() || !(tileEntity instanceof TileMultipart multipart)) {
            return null;
        }
        try {
            scala.collection.Iterator<TMultiPart> iterator = multipart.partList()
                .iterator();
            while (iterator.hasNext()) {
                TMultiPart part = iterator.next();
                if (part instanceof CableBusPart cableBusPart) {
                    return cableBusPart.getCableBus();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
