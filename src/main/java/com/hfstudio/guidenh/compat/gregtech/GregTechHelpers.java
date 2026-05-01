package com.hfstudio.guidenh.compat.gregtech;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.mixins.late.compat.gregtech.AccessorHatchElementBuilder;

import cpw.mods.fml.common.Optional;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.HatchElementBuilder;
import gregtech.common.blocks.ItemMachines;

public final class GregTechHelpers {

    public static final Logger LOG = LogManager.getLogger("GuideNH/GregTechHelpers");
    public static final Set<String> LOGGED_KEYS = Collections.synchronizedSet(new HashSet<>());

    private GregTechHelpers() {}

    public static ItemStack applyOreDictUnification(ItemStack stack) {
        if (stack == null || !Mods.GregTech.isModLoaded()) {
            return stack;
        }
        try {
            return applyOreDictUnificationImpl(stack);
        } catch (Throwable ignored) {
            return stack;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static ItemStack applyOreDictUnificationImpl(ItemStack stack) {
        ItemStack unified = GTOreDictUnificator.setStack(stack.copy());
        return unified != null ? unified : stack;
    }

    public static boolean registerDummyWorld(Class<?> worldClass) {
        if (worldClass == null || !Mods.GregTech.isModLoaded()) {
            return false;
        }
        try {
            registerDummyWorldImpl(worldClass);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static void registerDummyWorldImpl(Class<?> worldClass) {
        GregTechAPI.addDummyWorld(worldClass);
    }

    public static boolean isGregTechTileEntity(@Nullable TileEntity tileEntity) {
        return tileEntity != null && Mods.GregTech.isModLoaded() && isGregTechTileEntityImpl(tileEntity);
    }

    @Optional.Method(modid = "gregtech")
    private static boolean isGregTechTileEntityImpl(TileEntity tileEntity) {
        return tileEntity instanceof IGregTechTileEntity;
    }

    public static int resolveMetaTileId(@Nullable TileEntity tileEntity, int fallback) {
        if (!isGregTechTileEntity(tileEntity)) {
            return fallback;
        }
        try {
            int id = resolveMetaTileIdImpl(tileEntity);
            return id > 0 ? id : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static int resolveMetaTileIdImpl(TileEntity tileEntity) {
        return ((IGregTechTileEntity) tileEntity).getMetaTileID();
    }

    public static boolean hasValidMetaTileBinding(@Nullable TileEntity tileEntity) {
        if (!isGregTechTileEntity(tileEntity)) {
            return false;
        }
        try {
            return hasValidMetaTileBindingImpl(tileEntity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static boolean hasValidMetaTileBindingImpl(TileEntity tileEntity) {
        IMetaTileEntity meta = ((IGregTechTileEntity) tileEntity).getMetaTileEntity();
        if (meta == null) {
            return false;
        }
        return meta.getBaseMetaTileEntity() == tileEntity;
    }

    public static boolean repairMetaTileBinding(@Nullable TileEntity tileEntity) {
        if (!isGregTechTileEntity(tileEntity)) {
            return false;
        }
        try {
            return repairMetaTileBindingImpl(tileEntity);
        } catch (Throwable ignored) {
            logInfoOnce(
                "repair-exception:" + describeTile(tileEntity),
                "Exception while repairing GregTech MetaTileEntity binding: {}",
                describeTile(tileEntity));
            return false;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static boolean repairMetaTileBindingImpl(TileEntity tileEntity) {
        IGregTechTileEntity gtTile = (IGregTechTileEntity) tileEntity;
        IMetaTileEntity existingMeta = gtTile.getMetaTileEntity();
        if (existingMeta != null) {
            existingMeta.setBaseMetaTileEntity(gtTile);
            if (existingMeta.getBaseMetaTileEntity() == tileEntity) {
                logInfoOnce(
                    "repair-rebind:" + describeTile(tileEntity),
                    "Rebound existing GregTech MetaTileEntity: {}",
                    describeTile(tileEntity));
                return true;
            }
        }

        int metaTileId = gtTile.getMetaTileID();
        if (metaTileId <= 0) {
            logInfoOnce(
                "repair-missing-id:" + describeTile(tileEntity),
                "Cannot repair GregTech tile because no MetaTile id was available: {}",
                describeTile(tileEntity));
            return false;
        }

        NBTTagCompound snapshot = captureTileNbt(tileEntity, metaTileId);
        if (snapshot == null) {
            return false;
        }
        gtTile.setInitialValuesAsNBT(snapshot, (short) 0);
        boolean repaired = hasValidMetaTileBindingImpl(tileEntity);
        logInfoOnce(
            (repaired ? "repair-success:" : "repair-failed:") + describeTile(tileEntity),
            repaired ? "Recreated GregTech MetaTileEntity binding successfully: {}"
                : "GregTech MetaTileEntity binding was still invalid after recreation: {}",
            describeTile(tileEntity));
        return repaired;
    }

    @Nullable
    public static NBTTagCompound captureTileNbt(TileEntity tileEntity, int metaTileId) {
        try {
            NBTTagCompound snapshot = new NBTTagCompound();
            tileEntity.writeToNBT(snapshot);
            if (!snapshot.hasKey("mID")) {
                snapshot.setInteger("mID", metaTileId);
            }
            return snapshot;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Integer getMetaTileBaseType(int metaTileId) {
        if (metaTileId <= 0 || !Mods.GregTech.isModLoaded()) {
            return null;
        }
        try {
            return getMetaTileBaseTypeImpl(metaTileId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "gregtech")
    @Nullable
    private static Integer getMetaTileBaseTypeImpl(int metaTileId) {
        IMetaTileEntity[] entities = GregTechAPI.METATILEENTITIES;
        if (entities == null || metaTileId >= entities.length) {
            return null;
        }
        IMetaTileEntity entity = entities[metaTileId];
        if (entity == null) {
            return null;
        }
        return (int) entity.getTileEntityBaseType();
    }

    public static boolean initializeMetaTile(TileEntity tileEntity, int metaTileId, @Nullable NBTTagCompound tileTag) {
        if (tileEntity == null || metaTileId <= 0 || !Mods.GregTech.isModLoaded()) {
            return false;
        }
        try {
            return initializeMetaTileImpl(tileEntity, metaTileId, tileTag);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static boolean initializeMetaTileImpl(TileEntity tileEntity, int metaTileId,
        @Nullable NBTTagCompound tileTag) {
        if (!(tileEntity instanceof IGregTechTileEntity gtTile)) {
            return false;
        }
        NBTTagCompound payload = tileTag != null ? tileTag : new NBTTagCompound();
        gtTile.setInitialValuesAsNBT(payload, (short) metaTileId);
        return true;
    }

    public static void applyDefaultFacing(@Nullable TileEntity tileEntity, @Nullable NBTTagCompound tileTag) {
        if (tileEntity == null || !Mods.GregTech.isModLoaded()) {
            return;
        }
        if (tileTag != null && tileTag.hasKey("mFacing")) {
            return;
        }
        try {
            applyDefaultFacingImpl(tileEntity);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "gregtech")
    private static void applyDefaultFacingImpl(TileEntity tileEntity) {
        if (!(tileEntity instanceof IGregTechTileEntity gtTile)) {
            return;
        }
        ForgeDirection current = gtTile.getFrontFacing();
        if (current != null && current != ForgeDirection.UNKNOWN && gtTile.isValidFacing(current)) {
            return;
        }
        ForgeDirection[] preferred = new ForgeDirection[] { ForgeDirection.SOUTH, ForgeDirection.NORTH,
            ForgeDirection.EAST, ForgeDirection.WEST, ForgeDirection.UP, ForgeDirection.DOWN };
        for (ForgeDirection facing : preferred) {
            if (gtTile.isValidFacing(facing)) {
                gtTile.setFrontFacing(facing);
                return;
            }
        }
    }

    @Nullable
    public static Block getBlockMachines() {
        if (!Mods.GregTech.isModLoaded()) {
            return null;
        }
        try {
            return getBlockMachinesImpl();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "gregtech")
    @Nullable
    private static Block getBlockMachinesImpl() {
        return GregTechAPI.sBlockMachines;
    }

    @Nullable
    public static Object[] getMetaTileEntities() {
        if (!Mods.GregTech.isModLoaded()) {
            return null;
        }
        try {
            return getMetaTileEntitiesImpl();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "gregtech")
    @Nullable
    private static Object[] getMetaTileEntitiesImpl() {
        return GregTechAPI.METATILEENTITIES;
    }

    @Nullable
    public static Object getMetaTileEntityFromItem(@Nullable ItemStack stack) {
        if (stack == null || !Mods.GregTech.isModLoaded()) {
            return null;
        }
        try {
            return getMetaTileEntityFromItemImpl(stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "gregtech")
    @Nullable
    private static Object getMetaTileEntityFromItemImpl(ItemStack stack) {
        return ItemMachines.getMetaTileEntity(stack);
    }

    public static boolean isMTEHatch(@Nullable Object metaTileEntity) {
        return metaTileEntity != null && Mods.GregTech.isModLoaded() && isMTEHatchImpl(metaTileEntity);
    }

    @Optional.Method(modid = "gregtech")
    private static boolean isMTEHatchImpl(Object metaTileEntity) {
        return metaTileEntity instanceof MTEHatch;
    }

    public static boolean isHatchElementBuilder(@Nullable Object candidate) {
        return candidate != null && Mods.GregTech.isModLoaded() && isHatchElementBuilderImpl(candidate);
    }

    @Optional.Method(modid = "gregtech")
    private static boolean isHatchElementBuilderImpl(Object candidate) {
        return candidate instanceof HatchElementBuilder;
    }

    public static int getHatchBuilderHint(@Nullable Object hatchBuilder) {
        if (!isHatchElementBuilder(hatchBuilder)) {
            return -1;
        }
        try {
            return getHatchBuilderHintImpl(hatchBuilder);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static int getHatchBuilderHintImpl(Object hatchBuilder) {
        return ((AccessorHatchElementBuilder) (Object) hatchBuilder).guidenh$getHint();
    }

    @Nullable
    public static ItemStack getStackFormFromMetaTile(@Nullable Object metaTileEntity) {
        if (metaTileEntity == null || !Mods.GregTech.isModLoaded()) {
            return null;
        }
        try {
            return getStackFormFromMetaTileImpl(metaTileEntity);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "gregtech")
    @Nullable
    private static ItemStack getStackFormFromMetaTileImpl(Object metaTileEntity) {
        return ((IMetaTileEntity) metaTileEntity).getStackForm(1L);
    }

    public static void logInfoOnce(String key, String message, Object... args) {
        if (key == null || key.isEmpty() || message == null || message.isEmpty()) {
            return;
        }
        GuideDebugLog.runOnce(LOGGED_KEYS, key, () -> LOG.info(message, args));
    }

    public static String describeBlock(@Nullable Block block) {
        if (block == null) {
            return "null-block";
        }
        try {
            Object registryName = Block.blockRegistry.getNameForObject(block);
            if (registryName != null) {
                return registryName.toString();
            }
        } catch (Throwable ignored) {}
        try {
            return block.getUnlocalizedName();
        } catch (Throwable ignored) {
            return block.getClass()
                .getName();
        }
    }

    public static String describeTileTag(@Nullable NBTTagCompound tileTag) {
        if (tileTag == null) {
            return "null-tag";
        }
        StringBuilder builder = new StringBuilder();
        appendTagValue(builder, tileTag, "id");
        appendTagValue(builder, tileTag, "mID");
        appendTagValue(builder, tileTag, "mFacing");
        appendTagValue(builder, tileTag, "m");
        if (builder.length() == 0) {
            builder.append("empty-tag");
        }
        return builder.toString();
    }

    public static String describeTile(@Nullable TileEntity tileEntity) {
        if (tileEntity == null) {
            return "null-tile";
        }
        return tileEntity.getClass()
            .getName() + "@("
            + tileEntity.xCoord
            + ","
            + tileEntity.yCoord
            + ","
            + tileEntity.zCoord
            + ")"
            + " metaId="
            + resolveMetaTileId(tileEntity, -1)
            + " valid="
            + hasValidMetaTileBinding(tileEntity);
    }

    public static void appendTagValue(StringBuilder builder, NBTTagCompound tileTag, String key) {
        if (!tileTag.hasKey(key)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(key)
            .append('=');
        try {
            builder.append(tileTag.getTag(key));
        } catch (Throwable ignored) {
            builder.append("<unavailable>");
        }
    }
}
