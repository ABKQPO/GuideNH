package com.hfstudio.guidenh.integration.gregtech;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.mixins.late.compat.gregtech.AccessorHatchElementBuilder;

import cpw.mods.fml.common.Optional;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.MetaPipeEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.HatchElementBuilder;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.misc.GTStructureChannels;

public class GregTechHelpers {

    public static final Logger LOG = LogManager.getLogger("GuideNH/GregTechHelpers");
    public static final Set<String> LOGGED_KEYS = Collections.synchronizedSet(new HashSet<>());

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
        if (tileEntity == null) {
            return false;
        }
        try {
            return Mods.GregTech.isModLoaded() && isGregTechTileEntityImpl(tileEntity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static boolean isGregTechTileEntityImpl(TileEntity tileEntity) {
        return tileEntity instanceof IGregTechTileEntity;
    }

    public static boolean isMultiblockController(@Nullable TileEntity tileEntity) {
        if (!isGregTechTileEntity(tileEntity)) {
            return false;
        }
        try {
            return isMultiblockControllerImpl(tileEntity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static boolean isMultiblockControllerImpl(TileEntity tileEntity) {
        IGregTechTileEntity gtTile = (IGregTechTileEntity) tileEntity;
        IMetaTileEntity metaTileEntity = gtTile.getMetaTileEntity();
        return metaTileEntity instanceof MTEMultiBlockBase;
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

    public static boolean isMachineItem(@Nullable Item item) {
        return Mods.GregTech.isModLoaded() && isMachineItemImpl(item);
    }

    @Optional.Method(modid = "gregtech")
    private static boolean isMachineItemImpl(Item item) {
        return item instanceof ItemMachines;
    }

    public static boolean isMachineStack(@Nullable ItemStack stack) {
        return stack != null && Mods.GregTech.isModLoaded() && isMachineStackImpl(stack);
    }

    @Optional.Method(modid = "gregtech")
    private static boolean isMachineStackImpl(ItemStack stack) {
        return stack.getItem() instanceof ItemMachines && ItemMachines.getMetaTileEntity(stack) != null;
    }

    public static void appendMachineStacks(List<ItemStack> stacks) {
        if (stacks == null || !Mods.GregTech.isModLoaded()) {
            return;
        }
        try {
            appendMachineStacksImpl(stacks);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "gregtech")
    private static void appendMachineStacksImpl(List<ItemStack> stacks) {
        Block blockMachines = GregTechAPI.sBlockMachines;
        Object[] metaTileEntities = GregTechAPI.METATILEENTITIES;
        if (blockMachines == null || metaTileEntities == null) {
            return;
        }
        for (int meta = 1; meta < metaTileEntities.length; meta++) {
            if (metaTileEntities[meta] != null) {
                stacks.add(new ItemStack(blockMachines, 1, meta));
            }
        }
    }

    @Nullable
    public static Integer getMachineControllerBaseMeta(@Nullable Block block, int meta) {
        if (block == null || meta <= 0 || !Mods.GregTech.isModLoaded()) {
            return null;
        }
        try {
            return getMachineControllerBaseMetaImpl(block, meta);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "gregtech")
    @Nullable
    private static Integer getMachineControllerBaseMetaImpl(Block block, int meta) {
        Item item = Item.getItemFromBlock(block);
        if (!(item instanceof ItemMachines)) {
            return null;
        }
        IMetaTileEntity metaTileEntity = ItemMachines.getMetaTileEntity(new ItemStack(item, 1, meta));
        return metaTileEntity != null ? (int) metaTileEntity.getTileEntityBaseType() : null;
    }

    @Nullable
    public static TileEntity createMachineControllerTile(@Nullable World world, @Nullable Block block, int meta,
        @Nullable ItemStack stack) {
        if (world == null || block == null || meta <= 0 || !Mods.GregTech.isModLoaded()) {
            return null;
        }
        try {
            return createMachineControllerTileImpl(world, block, meta, stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "gregtech")
    @Nullable
    private static TileEntity createMachineControllerTileImpl(World world, Block block, int meta,
        @Nullable ItemStack stack) {
        Item item = Item.getItemFromBlock(block);
        if (!(item instanceof ItemMachines)) {
            return null;
        }
        ItemStack machineStack = stack != null ? stack.copy() : new ItemStack(item, 1, meta);
        IMetaTileEntity metaTileEntity = ItemMachines.getMetaTileEntity(machineStack);
        if (metaTileEntity == null) {
            return null;
        }
        int baseMeta = metaTileEntity.getTileEntityBaseType();
        TileEntity tileEntity = block.createTileEntity(world, baseMeta);
        if (!(tileEntity instanceof IGregTechTileEntity gtTile)) {
            return null;
        }
        return tileEntity;
    }

    public static boolean initializeMachineControllerTile(@Nullable TileEntity tileEntity, int meta,
        @Nullable ItemStack stack) {
        if (tileEntity == null || meta <= 0 || !Mods.GregTech.isModLoaded()) {
            return false;
        }
        try {
            return initializeMachineControllerTileImpl(tileEntity, meta, stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "gregtech")
    private static boolean initializeMachineControllerTileImpl(TileEntity tileEntity, int meta,
        @Nullable ItemStack stack) {
        if (!(tileEntity instanceof IGregTechTileEntity gtTile)) {
            return false;
        }
        ItemStack machineStack = stack != null ? stack.copy() : null;
        if (machineStack == null && tileEntity.getBlockType() != null) {
            Item item = Item.getItemFromBlock(tileEntity.getBlockType());
            if (item instanceof ItemMachines) {
                machineStack = new ItemStack(item, 1, meta);
            }
        }
        if (machineStack == null || ItemMachines.getMetaTileEntity(machineStack) == null) {
            return false;
        }
        gtTile.setInitialValuesAsNBT(machineStack.getTagCompound(), (short) meta);
        IMetaTileEntity createdMetaTileEntity = gtTile.getMetaTileEntity();
        if (createdMetaTileEntity != null) {
            createdMetaTileEntity.initDefaultModes(machineStack.getTagCompound());
            return true;
        }
        return false;
    }

    public static void applyPreviewControllerFacing(@Nullable TileEntity tileEntity) {
        if (tileEntity == null || !Mods.GregTech.isModLoaded()) {
            return;
        }
        try {
            applyPreviewControllerFacingImpl(tileEntity);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "gregtech")
    private static void applyPreviewControllerFacingImpl(TileEntity tileEntity) {
        if (!(tileEntity instanceof IGregTechTileEntity gtTile)) {
            return;
        }
        ForgeDirection previewFacing = defaultPreviewFacing();
        if (gtTile.isValidFacing(previewFacing)) {
            gtTile.setFrontFacing(previewFacing);
        }
    }

    public static void enableHatchPreviewChannel(@Nullable ItemStack triggerStack) {
        if (triggerStack == null || !Mods.GregTech.isModLoaded()) {
            return;
        }
        try {
            enableHatchPreviewChannelImpl(triggerStack);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "gregtech")
    private static void enableHatchPreviewChannelImpl(ItemStack triggerStack) {
        ChannelDataAccessor.setChannelData(triggerStack, GTStructureChannels.HATCH.get(), 1);
    }

    public static void synchronizeMultiblockPreviewState(@Nullable TileEntity controllerTile,
        @Nullable ItemStack triggerStack, boolean activeController, @Nullable List<String> warnings) {
        if (controllerTile == null || !Mods.GregTech.isModLoaded()) {
            return;
        }
        try {
            synchronizeMultiblockPreviewStateImpl(controllerTile, triggerStack, activeController, warnings);
        } catch (Throwable t) {
            logInfoOnce(
                "preview-state-sync-exception:" + describeTile(controllerTile),
                "GregTech preview state sync failed for {}",
                describeTile(controllerTile));
        }
    }

    @Optional.Method(modid = "gregtech")
    private static void synchronizeMultiblockPreviewStateImpl(TileEntity controllerTile,
        @Nullable ItemStack triggerStack, boolean activeController, @Nullable List<String> warnings) {
        if (!(controllerTile instanceof IGregTechTileEntity gtTile)) {
            return;
        }
        IMetaTileEntity metaTileEntity = gtTile.getMetaTileEntity();
        if (!(metaTileEntity instanceof MTEMultiBlockBase multiBlockBase)) {
            return;
        }

        try {
            boolean activeBefore = gtTile.isActive();
            Boolean machineBefore = readPreviewMachineState(multiBlockBase);
            multiBlockBase.clearHatches();
            boolean valid = multiBlockBase.checkMachine(gtTile, triggerStack);
            boolean machineApplied = applyPreviewMachineState(multiBlockBase, valid);
            Boolean machineAfter = readPreviewMachineState(multiBlockBase);
            if (!valid) {
                logInfoOnce(
                    "preview-state-sync-invalid:" + describeTile(controllerTile),
                    "GregTech preview state sync kept invalid structure state for {}",
                    describeTile(controllerTile));
            }
            if (shouldActivatePreviewController(activeController, valid)) {
                gtTile.setActive(true);
                gtTile.issueTextureUpdate();
                applyPreviewTextureUpdate(metaTileEntity);
            }
            GuideDebugLog.info(
                LOG,
                "GregTech preview sync controller={} meta={} facing={} valid={} activeRequested={} activeBefore={} activeAfter={} machineBefore={} machineAfter={} machineApplied={}",
                describeTile(controllerTile),
                describeMetaTile(metaTileEntity),
                describeFacing(gtTile),
                valid,
                activeController,
                activeBefore,
                gtTile.isActive(),
                machineBefore,
                machineAfter,
                machineApplied);
        } catch (Throwable t) {
            logInfoOnce(
                "preview-state-sync-failed:" + describeTile(controllerTile),
                "GregTech preview state sync could not finish for {}",
                describeTile(controllerTile));
        }
    }

    public static boolean applyPreviewMachineState(@Nullable Object multiBlockController, boolean formed) {
        if (multiBlockController == null) {
            return false;
        }
        for (Class<?> type = multiBlockController.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("mMachine");
                if (field.getType() != Boolean.TYPE) {
                    continue;
                }
                field.setAccessible(true);
                field.setBoolean(multiBlockController, formed);
                return true;
            } catch (NoSuchFieldException ignored) {
                continue;
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    public static boolean applyPreviewTextureUpdate(@Nullable Object metaTileEntity) {
        if (!(metaTileEntity instanceof IMetaTileEntity gregTechMetaTile)) {
            return false;
        }
        try {
            gregTechMetaTile.onTextureUpdate();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shouldActivatePreviewController(boolean activeControllerRequested, boolean validStructure) {
        return activeControllerRequested && validStructure;
    }

    @Nullable
    public static Boolean readPreviewMachineState(@Nullable Object multiBlockController) {
        if (multiBlockController == null) {
            return null;
        }
        for (Class<?> type = multiBlockController.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("mMachine");
                if (field.getType() != Boolean.TYPE) {
                    continue;
                }
                field.setAccessible(true);
                return field.getBoolean(multiBlockController);
            } catch (NoSuchFieldException ignored) {
                continue;
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
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
        ForgeDirection[] preferred = new ForgeDirection[] { defaultPreviewFacing(), ForgeDirection.SOUTH,
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
        return Mods.GregTech.isModLoaded() && isMTEHatchImpl(metaTileEntity);
    }

    @Optional.Method(modid = "gregtech")
    private static boolean isMTEHatchImpl(Object metaTileEntity) {
        return metaTileEntity instanceof MTEHatch;
    }

    public static boolean isHatchElementBuilder(@Nullable Object candidate) {
        return Mods.GregTech.isModLoaded() && isHatchElementBuilderImpl(candidate);
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
        return ((AccessorHatchElementBuilder) hatchBuilder).guidenh$getHint();
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
        if (builder.isEmpty()) {
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

    public static String describeMetaTile(@Nullable Object metaTileEntity) {
        if (metaTileEntity == null) {
            return "null-meta";
        }
        if (metaTileEntity instanceof IMetaTileEntity gregTechMetaTile) {
            try {
                ItemStack stackForm = gregTechMetaTile.getStackForm(1L);
                if (stackForm != null) {
                    return metaTileEntity.getClass()
                        .getName() + " stack="
                        + Item.itemRegistry.getNameForObject(stackForm.getItem())
                        + ":"
                        + stackForm.getItemDamage();
                }
            } catch (Throwable ignored) {}
        }
        return metaTileEntity.getClass()
            .getName();
    }

    public static String describeFacing(@Nullable IGregTechTileEntity gtTile) {
        if (gtTile == null) {
            return "null-facing";
        }
        try {
            ForgeDirection frontFacing = gtTile.getFrontFacing();
            return frontFacing != null ? frontFacing.name() : "null-facing";
        } catch (Throwable ignored) {
            return "unavailable-facing";
        }
    }

    public static void appendTagValue(StringBuilder builder, NBTTagCompound tileTag, String key) {
        if (!tileTag.hasKey(key)) {
            return;
        }
        if (!builder.isEmpty()) {
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

    public static void preparePipeConnections(GuidebookLevel level) {
        if (level == null || !Mods.GregTech.isModLoaded()) {
            return;
        }
        try {
            preparePipeConnectionsImpl(level);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "gregtech")
    private static void preparePipeConnectionsImpl(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (!(te instanceof BaseMetaPipeEntity basePipeEntity)) {
                continue;
            }
            IMetaTileEntity mte = basePipeEntity.getMetaTileEntity();
            if (!(mte instanceof MetaPipeEntity metaPipe)) {
                continue;
            }
            int x = te.xCoord;
            int y = te.yCoord;
            int z = te.zCoord;
            byte connections = 0;
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                TileEntity adj = level.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
                if (adj == null) {
                    continue;
                }
                if (isSamePipeType(metaPipe, adj)) {
                    connections |= dir.flag;
                    continue;
                }
                try {
                    if (metaPipe.canConnect(dir, adj)) {
                        connections |= dir.flag;
                    }
                } catch (Throwable ignored) {}
            }
            basePipeEntity.mConnections = connections;
        }
    }

    private static boolean isSamePipeType(MetaPipeEntity pipe, TileEntity adj) {
        if (!(adj instanceof BaseMetaPipeEntity adjBase)) {
            return false;
        }
        IMetaTileEntity adjMte = adjBase.getMetaTileEntity();
        if (adjMte == null) {
            return false;
        }
        return pipe.getClass()
            .isInstance(adjMte)
            || adjMte.getClass()
                .isInstance(pipe);
    }

    public static ForgeDirection defaultPreviewFacing() {
        return ForgeDirection.NORTH;
    }

    public static int defaultPreviewFacingMeta() {
        return defaultPreviewFacing().ordinal();
    }
}
