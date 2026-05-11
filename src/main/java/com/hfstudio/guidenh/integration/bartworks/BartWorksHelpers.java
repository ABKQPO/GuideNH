package com.hfstudio.guidenh.integration.bartworks;

import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.Mods;

import bartworks.common.tileentities.classic.TileEntityRotorBlock;
import cpw.mods.fml.common.Optional;

public class BartWorksHelpers {

    public BartWorksHelpers() {}

    public static boolean isRotorBlock(@Nullable TileEntity tileEntity) {
        return tileEntity != null && Mods.BartWorks.isModLoaded() && isRotorBlockImpl(tileEntity);
    }

    @Optional.Method(modid = "bartworks")
    private static boolean isRotorBlockImpl(TileEntity tileEntity) {
        return tileEntity instanceof TileEntityRotorBlock;
    }
}
